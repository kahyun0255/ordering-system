package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantInternalFacade {

    private final RestaurantValidationService restaurantValidationService;
    private final RestaurantAccessValidatorService restaurantAccessValidatorService;

    @Transactional(readOnly = true)
    public void validateRestaurant(UUID restaurantId, RestaurantValidationApplicationRequest restaurantValidationRequest) {
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        restaurantValidationService.validate(restaurant, restaurantValidationRequest);
    }

}
