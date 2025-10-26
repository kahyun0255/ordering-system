package com.orderingsystem.restaurant.domain.exception;

import com.orderingsystem.common.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
