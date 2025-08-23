package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.FindRestaurantService;
import com.orderingsystem.restaurant.application.dto.response.FindAllRestaurantsResponse;
import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<Page<FindAllRestaurantsResponse>> findAllRestaurants(
            @PageableDefault(size = 10, sort = "name", direction = Direction.ASC) Pageable pageable,
            @RequestParam(required = false) RestaurantStatus status) {
        return ResponseEntity.ok(findRestaurantService.findAllRestaurants(pageable, status));
    }

}
