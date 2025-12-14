package com.orderingsystem.coupon.application;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.coupon.application.dto.request.CreateCouponApplicationRequest;
import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponFacade {

    private final CreateCouponService createCouponService;
    private final CouponManagementService couponManagementService;
    private final CouponCachePort couponCachePort;

    public UUID createCoupon(CreateCouponApplicationRequest request, UserType userType, UUID userId) {
        if (!UserType.ADMIN.equals(userType)) {
            log.info("관리자가 아닌 유저가 쿠폰 생성 시도. UserId : [{}], UserType : [{}]", userId, userType);
            throw new AccessDeniedException("쿠폰 생성이 불가능합니다.");
        }

        return createCouponService.create(request, userId);
    }

    public void pauseCoupon(UUID couponId, UUID userId, UserType userType) {
        if (!UserType.ADMIN.equals(userType)) {
            log.info("관리자가 아닌 유저가 쿠폰 정지 시도. UserId : [{}], UserType : [{}]", userId, userType);
            throw new AccessDeniedException("쿠폰 정지가 불가능합니다.");
        }

        couponManagementService.pause(couponId, userId);
    }

    public void resumeCoupon(UUID couponId, UUID userId, UserType userType) {
        if (!UserType.ADMIN.equals(userType)) {
            log.info("관리자가 아닌 유저가 쿠폰 재시작 시도. UserId : [{}], UserType : [{}]", userId, userType);
            throw new AccessDeniedException("쿠폰 재시작이 불가능합니다.");
        }

        couponManagementService.resume(couponId, userId);
    }
}
