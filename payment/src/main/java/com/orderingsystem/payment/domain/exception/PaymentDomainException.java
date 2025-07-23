package com.orderingsystem.payment.domain.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.orderingsystem.common.exception.DomainException;

public class PaymentDomainException extends DomainException {

    public PaymentDomainException(String message) {
        super(message);
    }

    public PaymentDomainException(String message, JsonProcessingException e) {
        super(message);
    }

}
