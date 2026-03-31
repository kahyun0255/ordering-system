package com.orderingsystem.restaurant.presentation.admin;

import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.restaurant.application.admin.RestaurantAdminService;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.presentation.request.UpdateRestaurantRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/restaurants")
@RequiredArgsConstructor
public class RestaurantAdminController {

    private final CommonJwtUtil commonJwtUtil;
    private final RestaurantAdminService restaurantAdminService;

    @PatchMapping("/{restaurantId}")
    public ResponseEntity<UpdateRestaurantResponse> updateRestaurant(@PathVariable UUID restaurantId,
                                                                     @RequestHeader(value = "Authorization") String authorizationHeader,
                                                                     @Valid @RequestBody UpdateRestaurantRequest updateRestaurantRequest) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);

        return ResponseEntity.ok(restaurantAdminService.updateRestaurant(userId, restaurantId,
                updateRestaurantRequest.toUpdateRestaurantApplicationRequest()));
    }

}
