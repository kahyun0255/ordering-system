package com.orderingsystem.restaurant.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FindRestaurantResponse {

    private String name;
    private Boolean active;

}
