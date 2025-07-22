package com.orderingsystem.payment.application.exception;

public class PaymentApplicationException extends RuntimeException {
    public PaymentApplicationException(String message) {
        super(message);
    }
}
