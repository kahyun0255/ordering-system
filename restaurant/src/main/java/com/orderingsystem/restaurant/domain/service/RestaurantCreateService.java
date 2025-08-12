package com.orderingsystem.restaurant.domain.service;

import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class RestaurantCreateService {

    public RestaurantCreateResult create(Owner owner, String name) {
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name(name)
                .active(true)
                .build();

        RestaurantOwnership restaurantOwnership = RestaurantOwnership.builder()
                .ownerId(owner.getUserId())
                .restaurantId(restaurantId)
                .build();

        CreatedRestaurantEvent createdRestaurantEvent = new CreatedRestaurantEvent(restaurant, ZonedDateTime.now());

        return new RestaurantCreateResult(restaurant, restaurantOwnership, createdRestaurantEvent);
    }

    @Getter
    @Builder
    public static class RestaurantCreateResult {
        private final Restaurant restaurant;
        private final RestaurantOwnership restaurantOwnership;
        private final CreatedRestaurantEvent createdRestaurantEvent;
    }
}
