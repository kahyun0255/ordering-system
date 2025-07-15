package com.orderingsystem.payment.domain.exception;

import com.orderingsystem.common.exceptioin.DomainException;

public class PaymentDomainException extends DomainException {

    public PaymentDomainException(String message) {
        super(message);
    }
}
