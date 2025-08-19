package com.orderingsystem.restaurant.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantApplicationRequest {

    private String name;

}
