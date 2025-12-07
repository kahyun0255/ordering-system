package com.orderingsystem.coupon.domain.exception;

import com.orderingsystem.common.exception.NotFoundException;

public class CouponNotFoundException extends NotFoundException {
    public CouponNotFoundException(String message) {
        super(message);
    }
}
