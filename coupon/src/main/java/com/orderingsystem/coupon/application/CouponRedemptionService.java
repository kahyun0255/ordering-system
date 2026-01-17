package com.orderingsystem.coupon.application;

import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.coupon.application.dto.request.CouponRequest;
import com.orderingsystem.coupon.application.mapper.CouponDataMapper;
import com.orderingsystem.coupon.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.coupon.domain.exception.CouponApplicationException;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponRedemptionService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final CouponDataMapper couponDataMapper;

    @Transactional
    public void redeem(CouponRequest request) {
        log.info("쿠폰 사용 처리 시작. Order Id : [{}], Issued Coupon Id : [{}], User Id : [{}]",
                request.getOrderId(), request.getIssuedCouponIds().toString(), request.getUserId());

        List<String> failureMessage = new ArrayList<>();
        int updatedCount = 0;

        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllById(request.getIssuedCouponIds());

        boolean isValid = validCoupons(request, issuedCoupons, failureMessage);

        if (isValid) {
            updatedCount = executeRedemption(request, failureMessage);
        }
        saveResultToOutbox(request, updatedCount, failureMessage);

        log.info("쿠폰 사용 처리 완료. OrderId: [{}], Success: [{}], FailureMessages: [{}]", request.getOrderId(),
                (updatedCount > 0), failureMessage);
    }

    private boolean validCoupons(CouponRequest request, List<IssuedCoupon> issuedCoupons,
                                 List<String> failureMessage) {
        if (issuedCoupons.size() != request.getIssuedCouponIds().size()) {
            log.info("존재하지 않는 쿠폰이 포함된 사용 요청. User Id : [{}], Coupon Ids : [{}]",
                    request.getUserId(), request.getIssuedCouponIds().toString());
            failureMessage.add("존재하지 않는 쿠폰이 포함되어 있습니다.");
            return false;
        }

        issuedCoupons.forEach(ic -> {
            if (!ic.getUserId().equals(request.getUserId())) {
                log.info("[{}] 쿠폰을 발급받지 않은 유저가 사용 요청. User Id : [{}]",
                        request.getIssuedCouponIds(), request.getUserId());
                failureMessage.add("쿠폰을 발급한 유저만 사용이 가능합니다.");
                return;
            }

            if (!ic.getDisplayStatus().equals(IssuedCouponStatus.ISSUED)) {
                log.info("쿠폰이 사용 불가능한 상태. Status : [{}], Issued Coupon Id : [{}], User Id : [{}]",
                        ic.getStatus(), request.getIssuedCouponIds(), request.getUserId());
                failureMessage.add("쿠폰이 사용 불가능한 상태입니다.");
                return;
            }
        });

        return failureMessage.isEmpty();
    }

    private int executeRedemption(CouponRequest request, List<String> failureMessage) {
        int updatedCount = issuedCouponRepository.redeemCoupons(IssuedCouponStatus.USED, request.getOrderId(),
                LocalDateTime.now(), request.getIssuedCouponIds(), IssuedCouponStatus.ISSUED);

        if (updatedCount != request.getIssuedCouponIds().size()) {
            log.error("쿠폰 동시성 이슈 발생 또는 상태 불일치. Request Count : [{}], Updated Count : [{}], Order Id : [{}], "
                            + "User Id : [{}], Coupon Id : [{}]",
                    request.getIssuedCouponIds().size(), updatedCount, request.getOrderId(), request.getUserId(),
                    request.getIssuedCouponIds());
            failureMessage.add("쿠폰 사용 중 오류 발생.");
        }
        return updatedCount;
    }

    private void saveResultToOutbox(CouponRequest request, int updatedCount, List<String> failureMessage) {
        orderOutboxHelper.saveOrderOutboxMessage(
                couponDataMapper.redeemCouponToCouponOrderEventPayload(request, updatedCount, failureMessage),
                SagaStatus.PROCESSING,
                request.getSagaId()
        );
    }

    @Transactional
    public void cancelRedemption(CouponRequest request) {
        log.info("쿠폰 사용 취소 처리 시작. Order Id : [{}], Issued Coupon Id : [{}], User Id : [{}]",
                request.getOrderId(), request.getIssuedCouponIds().toString(), request.getUserId());

        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllById(request.getIssuedCouponIds());

        validCancelRedemptionCoupons(request, issuedCoupons);
        int updatedCount = executeCancelRedemption(request);

        log.info("쿠폰 사용 취소 처리 완료. OrderId: [{}], Success: [{}]", request.getOrderId(), (updatedCount > 0));
    }

    private void validCancelRedemptionCoupons(CouponRequest request, List<IssuedCoupon> issuedCoupons) {
        if (issuedCoupons.size() != request.getIssuedCouponIds().size()) {
            log.info("존재하지 않는 쿠폰이 포함된 사용 취소 요청. User Id : [{}], Coupon Ids : [{}]",
                    request.getUserId(), request.getIssuedCouponIds().toString());
            throw new CouponApplicationException("취소를 요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");
        }

        List<Long> invalidCouponIds = issuedCoupons.stream()
                .filter(ic -> !request.getOrderId().equals(ic.getOrderId())
                        || ic.getDisplayStatus() != IssuedCouponStatus.USED)
                .map(IssuedCoupon::getId)
                .toList();

        if (!invalidCouponIds.isEmpty()) {
            log.info("쿠폰 사용 취소 요청 중 해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다. "
                            + "Order Id: [{}], Invalid Coupon Ids: [{}], User Id: [{}]",
                    request.getOrderId(), invalidCouponIds, request.getUserId());

            throw new CouponApplicationException("해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다.");
        }
    }

    private int executeCancelRedemption(CouponRequest request) {
        int updatedCount = issuedCouponRepository.redeemCancelCoupons(IssuedCouponStatus.ISSUED,
                request.getIssuedCouponIds(), IssuedCouponStatus.USED);

        if (updatedCount != request.getIssuedCouponIds().size()) {
            log.error("쿠폰 취소 동시성 이슈 발생 또는 상태 불일치. Request Count : [{}], Updated Count : [{}], Order Id : [{}], "
                            + "User Id : [{}], Coupon Id : [{}]",
                    request.getIssuedCouponIds().size(), updatedCount, request.getOrderId(), request.getUserId(),
                    request.getIssuedCouponIds());
            throw new CouponApplicationException("쿠폰 취소 실패.");
        }
        return updatedCount;
    }

}
