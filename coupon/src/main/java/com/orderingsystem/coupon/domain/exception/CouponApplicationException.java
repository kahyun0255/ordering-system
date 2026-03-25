package com.orderingsystem.coupon.domain.exception;

public class CouponApplicationException extends RuntimeException {

    public CouponApplicationException(String message) {
        super(message);
    }

    public CouponApplicationException(String message, Exception e) {
        super(message, e);
    }

}
