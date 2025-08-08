package com.orderingsystem.domain;

import com.orderingsystem.common.exception.DomainException;

public class UserDomainException extends DomainException {
    public UserDomainException(String message) {
        super(message);
    }
}
