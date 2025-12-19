package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.application.dto.response.CouponValidationResponse;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import com.orderingsystem.coupon.presentation.request.CouponValidationRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponValidationService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public CouponValidationResponse validateAndCalculate(CouponValidationRequest request) {
        UUID userId = request.getCustomerId();
        UUID sagaId = request.getSagaId();

        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllById(request.getCouponIds());

        if (issuedCoupons.size() != request.getCouponIds().size()) {
            log.info("[{}] 유저가 사용 요청한 쿠폰 중 존재하지 않는 쿠폰이 포함됨. SagaId : [{}]", userId, sagaId);
            return failResponse("요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");
        }

        Map<UUID, Coupon> couponMap = createCouponMap(issuedCoupons);

        BigDecimal discountAmount = BigDecimal.ZERO;

        for (IssuedCoupon ic : issuedCoupons) {
            Coupon coupon = couponMap.get(ic.getCouponId());

            if (!isOwner(ic, userId, sagaId)) {
                return failResponse("본인의 쿠폰만 사용할 수 있습니다.");
            }

            String statusError = validateStatus(ic, userId, sagaId);
            if (statusError != null) {
                return failResponse(statusError);
            }

            if (!isMinOrderAmountSatisfied(coupon, request.getTotalOrderAmount(), userId, sagaId)) {
                return failResponse("최소 주문 금액을 만족하지 못했습니다.");
            }

            discountAmount = discountAmount.add(calculateDiscount(coupon, request.getTotalOrderAmount()));
        }

        BigDecimal finalPayAmount = calculateFinalPayAmount(request.getTotalOrderAmount(), discountAmount);

        return CouponValidationResponse.builder()
                .valid(true)
                .discountAmount(discountAmount)
                .finalAmount(finalPayAmount)
                .message(null)
                .build();
    }

    private Map<UUID, Coupon> createCouponMap(List<IssuedCoupon> issuedCoupons) {
        List<UUID> couponIds = issuedCoupons.stream()
                .map(IssuedCoupon::getCouponId)
                .toList();

        return couponRepository.findAllById(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getCouponId, Function.identity()));
    }

    private boolean isOwner(IssuedCoupon ic, UUID userId, UUID sagaId) {
        if (!ic.getUserId().equals(userId)) {
            log.info("[{}] 유저가 본인의 소유가 아닌 쿠폰 사용 요청. SagaId : [{}]", userId, sagaId);
            return false;
        }
        return true;
    }

    private String validateStatus(IssuedCoupon ic, UUID userId, UUID sagaId) {
        switch (ic.getStatus()) {
            case USED -> {
                log.info("[{}] 유저가 이미 사용한(USED) 쿠폰 사용 요청. SagaId : [{}], CouponId : [{}]",
                        userId, sagaId, ic.getCouponId());
                return "이미 사용한 쿠폰입니다.";
            }
            case EXPIRED -> {
                log.info("[{}] 유저가 만료된(EXPIRED) 쿠폰 사용 요청. SagaId : [{}], CouponId : [{}]",
                        userId, sagaId, ic.getCouponId());
                return "만료된 쿠폰입니다.";
            }
            case REVOKED -> {
                log.info("[{}] 유저가 사용 불가능한(REVOKED) 쿠폰 사용 요청. SagaId : [{}], CouponId : [{}]",
                        userId, sagaId, ic.getCouponId());
                return "사용이 불가능한 쿠폰입니다.";
            }
        }

        if (ic.getDisplayStatus().equals(IssuedCouponStatus.EXPIRED)) {
            log.info("[{}] 유저가 만료된(EXPIRED) 쿠폰 사용 요청. SagaId : [{}], CouponId : [{}]",
                    userId, sagaId, ic.getCouponId());
            return "만료된 쿠폰입니다.";
        }

        return null;
    }

    private boolean isMinOrderAmountSatisfied(Coupon coupon, BigDecimal totalOrderAmount, UUID userId, UUID sagaId) {
        if (coupon.getMinDiscountAmount() != null && totalOrderAmount.compareTo(coupon.getMinDiscountAmount()) < 0) {
            log.info("[{}] 유저의 주문이 최소 주문 금액을 만족하지 못했습니다. 최소 주문 금액 : [{}], 현재 주문 금액 : [{}], SagaId : [{}]",
                    userId, coupon.getMinDiscountAmount(), totalOrderAmount, sagaId);
            return false;
        }
        return true;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal totalOrderAmount) {
        switch (coupon.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal calc = totalOrderAmount
                        .multiply(BigDecimal.valueOf(coupon.getPercentOff()))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                return coupon.getMaxDiscountAmount() != null ? calc.min(coupon.getMaxDiscountAmount()) : calc;
            }
            case FIXED_AMOUNT -> {
                return coupon.getAmountOff();
            }
            default -> {
                return BigDecimal.ZERO;
            }
        }
    }

    private BigDecimal calculateFinalPayAmount(BigDecimal totalOrderAmount, BigDecimal discountAmount) {
        BigDecimal finalAmount = totalOrderAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        return finalAmount;
    }

    private CouponValidationResponse failResponse(String message) {
        return CouponValidationResponse.builder()
                .valid(false)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(null)
                .message(message)
                .build();
    }
}
