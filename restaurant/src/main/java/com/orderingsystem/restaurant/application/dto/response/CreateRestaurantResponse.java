package com.orderingsystem.restaurant.application.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRestaurantResponse {

    private UUID restaurantId;
    private String message;

}
