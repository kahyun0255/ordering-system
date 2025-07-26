package com.orderingsystem.payment.application.exception;

public class PaymentApplicationException extends RuntimeException {
    public PaymentApplicationException(String message) {
        super(message);
    }

    public PaymentApplicationException(String message, Exception e) {
        super(message, e);
    }
}
