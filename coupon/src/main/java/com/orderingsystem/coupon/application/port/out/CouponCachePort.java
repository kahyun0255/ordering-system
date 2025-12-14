package com.orderingsystem.coupon.application.port.out;

import java.util.UUID;

public interface CouponCachePort {

    long currentStock(UUID couponId);

    Long decreaseStock(UUID couponId);

    Long increaseStock(UUID couponId);

    boolean addIssuedUser(UUID couponId, UUID userId);

    void removeIssuedUser(UUID couponId, UUID userId);

    boolean exists(UUID couponId);

    void deleteCouponStock(UUID couponId);

}
