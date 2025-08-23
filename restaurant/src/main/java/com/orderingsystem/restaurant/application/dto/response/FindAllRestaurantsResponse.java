package com.orderingsystem.restaurant.application.dto.response;

import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FindAllRestaurantsResponse {

    private UUID restaurantId;
    private String restaurantName;
    private RestaurantStatus status;

}
