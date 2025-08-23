package com.orderingsystem.restaurant.domain.event.restaruant;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.time.ZonedDateTime;

public class CreatedRestaurantEvent extends RestaurantEvent {
    public CreatedRestaurantEvent(Restaurant restaurant, ZonedDateTime createdAt) {
        super(restaurant, createdAt);
    }
}
