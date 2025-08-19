package com.orderingsystem.restaurant.presentation.request;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRestaurantRequest {

    private String name;

    public UpdateRestaurantApplicationRequest toUpdateRestaurantApplicationRequest() {
        return UpdateRestaurantApplicationRequest.builder()
                .name(this.name)
                .build();
    }
}
