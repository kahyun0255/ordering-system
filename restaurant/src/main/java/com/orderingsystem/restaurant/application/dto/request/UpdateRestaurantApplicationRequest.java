package com.orderingsystem.restaurant.application.dto.request;

import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantApplicationRequest {

    private String name;
    private RestaurantStatus status;

}
