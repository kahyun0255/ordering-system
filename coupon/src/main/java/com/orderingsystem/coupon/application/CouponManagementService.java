package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
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

    @Transactional
    public void pause(UUID couponId, UUID userId) {
        log.info("[{}] 유저가 [{}] 쿠폰 정지.", userId, couponId);

        Coupon coupon = getCoupon(couponId, userId);
        coupon.pause();
    }

    private Coupon getCoupon(UUID couponId, UUID userId) {
        Optional<Coupon> coupon = couponRepository.findById(couponId);
        if (coupon.isEmpty()){
            log.info("[{}] 쿠폰이 존재하지 않습니다. 조회한 유저 : [{}]", coupon, userId);
            throw new CouponNotFoundException("쿠폰이 존재하지 않습니다.");
        }

        return coupon.get();
    }

}
