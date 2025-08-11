package com.orderingsystem.domain.exception;

import com.orderingsystem.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
