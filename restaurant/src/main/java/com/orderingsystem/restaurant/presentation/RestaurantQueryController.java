package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.RestaurantQueryService;
import com.orderingsystem.restaurant.application.dto.response.RestaurantInfoResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
public class RestaurantQueryController {

    private final RestaurantQueryService restaurantQueryService;

    @GetMapping("/{restaurantId}/products")
    public ResponseEntity<RestaurantInfoResponse> getRestaurantInfo(@PathVariable UUID restaurantId, @RequestParam List<UUID> productIds){
        return ResponseEntity.ok(restaurantQueryService.getRestaurantInfo(restaurantId, productIds));
    }
}
