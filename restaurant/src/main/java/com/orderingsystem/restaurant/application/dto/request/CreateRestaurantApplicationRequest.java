package com.orderingsystem.restaurant.application.dto.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRestaurantApplicationRequest {

    private UUID ownerId;
    private String name;

}
