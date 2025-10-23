package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest;
import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest.Item;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantInternalFacade {

    private final RestaurantValidationService restaurantValidationService;
    private final RestaurantAccessValidatorService restaurantAccessValidatorService;
    private final ProductStockFacade productStockFacade;

    public void validateRestaurant(UUID restaurantId, RestaurantValidationApplicationRequest restaurantValidationRequest) {
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        restaurantValidationService.validate(restaurant, restaurantValidationRequest);

        for (Item item : restaurantValidationRequest.getItems()){
            productStockFacade.reserve(item.getProductId(), item.getQuantity(), restaurantValidationRequest.getSagaId());
        }
    }

}
