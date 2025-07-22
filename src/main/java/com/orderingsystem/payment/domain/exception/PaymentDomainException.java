package com.orderingsystem.payment.domain.exception;

import com.orderingsystem.common.exception.DomainException;

public class PaymentDomainException extends DomainException {

    public PaymentDomainException(String message) {
        super(message);
    }
}
