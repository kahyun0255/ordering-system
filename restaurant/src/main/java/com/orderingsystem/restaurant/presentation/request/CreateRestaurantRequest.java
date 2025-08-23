package com.orderingsystem.restaurant.presentation.request;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRestaurantRequest {

    @NotBlank(message = "레스토랑 이름은 필수입니다.")
    private String name;

    public CreateRestaurantApplicationRequest toCreateRestaurantApplicationRequest(UUID restaurantOwnerId){
        return CreateRestaurantApplicationRequest.builder()
                .ownerId(restaurantOwnerId)
                .name(this.name)
                .build();
    }
}
