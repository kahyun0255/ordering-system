package com.orderingsystem.restaurant.presentation.request;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantRequest {

    private String name;
    private RestaurantStatus status;

    public UpdateRestaurantApplicationRequest toUpdateRestaurantApplicationRequest() {
        return UpdateRestaurantApplicationRequest.builder()
                .status(status)
                .name(this.name)
                .build();
    }
}
