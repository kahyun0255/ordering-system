package com.orderingsystem.coupon.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponIssuedEvent {

    private final UUID couponId;
    private final UUID userId;
    private final LocalDateTime createdAt;

}
