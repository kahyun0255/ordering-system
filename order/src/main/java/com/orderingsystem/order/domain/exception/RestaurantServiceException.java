package com.orderingsystem.order.domain.exception;

public class RestaurantServiceException extends RuntimeException {
    public RestaurantServiceException(String message) {
        super(message);
    }
}
