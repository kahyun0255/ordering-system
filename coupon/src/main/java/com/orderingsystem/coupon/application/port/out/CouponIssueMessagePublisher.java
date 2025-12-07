package com.orderingsystem.coupon.application.port.out;

import com.orderingsystem.coupon.domain.event.CouponIssuedEvent;

public interface CouponIssueMessagePublisher {
    void publish(CouponIssuedEvent event);
}
