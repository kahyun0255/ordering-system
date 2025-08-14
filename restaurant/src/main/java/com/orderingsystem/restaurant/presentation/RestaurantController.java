package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.FindRestaurantService;
import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {
    private final FindRestaurantService findRestaurantService;

    @GetMapping("/{restaurantId}")
    public ResponseEntity<FindRestaurantResponse> findRestaurant(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(findRestaurantService.findRestaurant(restaurantId));
    }

}
