package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.application.dto.response.CouponValidateResponse;
import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.presentation.request.ValidationCouponRequest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponManagementService {

    private final CouponRepository couponRepository;
    private final CouponCachePort couponCachePort;

    @Transactional
    public void pause(UUID couponId, UUID userId) {
        log.info("[{}] 유저가 [{}] 쿠폰 정지.", userId, couponId);

        Coupon coupon = getCoupon(couponId, userId);
        coupon.pause();

        couponCachePort.deleteCouponStock(couponId);
    }

    @Transactional
    public void resume(UUID couponId, UUID userId) {
        log.info("[{}] 유저가 [{}] 쿠폰 재시작.", userId, couponId);

        Coupon coupon = getCoupon(couponId, userId);
        coupon.resume();

        couponCachePort.enableCoupon(couponId, coupon.getIssueLimit() - coupon.getIssuedCount(),
                coupon.getValidUntil());
    }

    @Transactional
    public void terminate(UUID couponId, UUID userId) {
        log.info("[{}] 유저가 [{}] 쿠폰 종료.", userId, couponId);

        Coupon coupon = getCoupon(couponId, userId);
        coupon.terminate();

        couponCachePort.deleteCouponStock(couponId);
        couponCachePort.setExpireIssuedUserKey(couponId);
    }

    private Coupon getCoupon(UUID couponId, UUID userId) {
        Optional<Coupon> coupon = couponRepository.findById(couponId);
        if (coupon.isEmpty()) {
            log.info("[{}] 쿠폰이 존재하지 않습니다. 조회한 유저 : [{}]", coupon, userId);
            throw new CouponNotFoundException("쿠폰이 존재하지 않습니다.");
        }

        return coupon.get();
    }

    public CouponValidateResponse aa(ValidationCouponRequest request) {
        return CouponValidateResponse.builder()
                .valid(true)
                .discountAmount(BigDecimal.valueOf(1000))
                .finalAmount(BigDecimal.valueOf(9000))
                .message("성공!")
                .build();
    }

}
