package com.orderingsystem.restaurant.application.dto.response;

import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantResponse {

    private UUID restaurantId;
    private String name;
    private RestaurantStatus status;
    private String message;

}
