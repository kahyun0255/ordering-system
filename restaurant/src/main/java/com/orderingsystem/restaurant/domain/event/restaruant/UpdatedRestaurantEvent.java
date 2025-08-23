package com.orderingsystem.restaurant.domain.event.restaruant;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.time.ZonedDateTime;

public class UpdatedRestaurantEvent extends RestaurantEvent{
    public UpdatedRestaurantEvent(Restaurant restaurant,
                                  ZonedDateTime createdAt) {
        super(restaurant, createdAt);
    }
}
