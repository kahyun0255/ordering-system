package com.orderingsystem.coupon.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CouponSchedulerPort {

    void scheduleCouponStart(UUID couponId, Long issueLimit, LocalDateTime validFrom, LocalDateTime validUntil);

}
