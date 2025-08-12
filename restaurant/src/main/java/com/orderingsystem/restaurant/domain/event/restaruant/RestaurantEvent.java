package com.orderingsystem.restaurant.domain.event.restaruant;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class RestaurantEvent implements DomainEvent<Restaurant> {

    private final Restaurant restaurant;
    private final ZonedDateTime createdAt;

}
