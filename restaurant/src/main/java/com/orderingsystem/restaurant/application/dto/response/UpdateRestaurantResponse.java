package com.orderingsystem.restaurant.application.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantResponse {

    private UUID restaurantId;
    private String name;
    private Boolean active;
    private String message;

}
