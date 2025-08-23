package com.orderingsystem.restaurant.application.dto.response;

import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FindRestaurantResponse {

    private String name;
    private RestaurantStatus status;

}
