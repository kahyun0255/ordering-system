package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.RestaurantInternalFacade;
import com.orderingsystem.restaurant.presentation.request.RestaurantValidationRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/restaurants")
@RequiredArgsConstructor
public class RestaurantInternalController {

    private final RestaurantInternalFacade restaurantInternalFacade;

    @PostMapping("/{restaurantId}/validate")
    public ResponseEntity<Void> validateRestaurantAndProducts(@PathVariable UUID restaurantId,
                                                        @RequestBody RestaurantValidationRequest restaurantValidationRequest) {
        restaurantInternalFacade.validateRestaurant(restaurantId,
                restaurantValidationRequest.toRestaurantValidationApplicationRequest());
        return ResponseEntity.noContent().build();
    }

}
