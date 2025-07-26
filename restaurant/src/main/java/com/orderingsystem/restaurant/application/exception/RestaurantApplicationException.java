package com.orderingsystem.restaurant.application.exception;

public class RestaurantApplicationException extends RuntimeException {
    public RestaurantApplicationException(String message) {
        super(message);
    }

    public RestaurantApplicationException(String message, Exception e) {
        super(message, e);
    }
}
