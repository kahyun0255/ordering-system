package com.orderingsystem.restaurant.domain.exception;

import com.orderingsystem.common.exception.NotFoundException;

public class RestaurantNotFoundException extends NotFoundException {
    public RestaurantNotFoundException(String message) {
        super(message);
    }
}
