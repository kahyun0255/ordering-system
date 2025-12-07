package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.application.dto.request.CouponIssueApplicationRequest;
import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import com.orderingsystem.coupon.application.port.out.CouponIssueMessagePublisher;
import com.orderingsystem.coupon.domain.event.CouponIssuedEvent;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueCouponService {

    private final CouponCachePort couponCachePort;
    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final CouponIssueMessagePublisher couponIssueMessagePublisher;

    public void requestCouponIssuance(UUID couponId, UUID userId, LocalDateTime issuanceRequestedAt) {
        ensureCouponExists(couponId, userId);
        ensureFirstIssue(couponId, userId);
        Long remainingStock = couponCachePort.decreaseStock(couponId);

        CouponIssuedEvent couponIssuedEvent = CouponIssuedEvent.builder()
                .couponId(couponId)
                .createdAt(issuanceRequestedAt)
                .userId(userId)
                .build();

        if (remainingStock != null && remainingStock >= 0) {
            try {
                couponIssueMessagePublisher.publish(couponIssuedEvent);
            } catch (Exception e) {
                //TODO: Kafka 롤백(재고 복구, 중복 이력 삭제)
                couponCachePort.increaseStock(couponId);
                couponCachePort.removeIssuedUser(couponId, userId);

                log.warn("쿠폰 발급 중 Kafka 발행 실패로 인한 오류 발생. 롤백 진행. couponId : [{}], userId : [{}]", couponId, userId);
                throw new RuntimeException("쿠폰 발급 중 오류 발생");
            }
        } else {
            couponCachePort.increaseStock(couponId);
            couponCachePort.removeIssuedUser(couponId, userId);
            throw new IllegalArgumentException("쿠폰이 마감되었습니다.");
        }

        log.info("쿠폰 발급 성공. couponId : [{}], userId : [{}], 남은 재고 : [{}]", couponId, userId, remainingStock);
    }

    private void ensureCouponExists(UUID couponId, UUID userId) {
        if (!couponCachePort.exists(couponId)){
            log.info("존재하지 않는 쿠폰 발급 요청. couponId : [{}], userId : [{}]", couponId, userId);
            throw new CouponNotFoundException("존재하지 않는 쿠폰입니다.");
        }
    }

    private void ensureFirstIssue(UUID couponId, UUID userId) {
        boolean isNewUser = couponCachePort.addIssuedUser(couponId, userId);
        if (!isNewUser) {
            log.warn("이미 쿠폰이 발급된 사용자입니다. couponId:[{}], userId:[{}]", couponId, userId);
            throw new IllegalArgumentException("이미 쿠폰이 발급되었습니다.");
        }
    }

    @Transactional
    public void saveIssuedCoupon(List<CouponIssueApplicationRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        List<IssuedCoupon> issuedCoupons = new ArrayList<>();

        for (CouponIssueApplicationRequest request : requests) {
            log.info("[{}] 유저에 대한 쿠폰 [{}] 저장.", request.getCouponId(), request.getUserId());

            IssuedCoupon issuedCoupon = IssuedCoupon.create(request.getUserId(), request.getCouponId(),
                    request.getIssuedAt(), request.getExpiredAt());
            issuedCoupons.add(issuedCoupon);
        }
        issuedCouponRepository.saveAll(issuedCoupons);

        Map<UUID, Long> countMap = requests.stream()
                .collect(Collectors.groupingBy(CouponIssueApplicationRequest::getCouponId, Collectors.counting()));

        countMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> couponRepository.increaseIssuedCount(entry.getKey(), entry.getValue()));
    }

}
