package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.restaurant.application.RestaurantManagementFacade;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.presentation.request.CreateRestaurantRequest;
import com.orderingsystem.restaurant.presentation.request.UpdateRestaurantRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantManagementController {

    private final RestaurantManagementFacade restaurantManagementFacade;
    private final RestaurantControllerHelper restaurantControllerHelper;

    @PostMapping
    public ResponseEntity<CreateRestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest createRestaurantRequest, BindingResult bindingResult,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        RestaurantControllerHelper.valid(bindingResult);
        UUID restaurantOwnerId = restaurantControllerHelper.getRestaurantOwnerId(authorizationHeader);

        log.info("레스토랑 생성. Restaurant Owner Id : {}", restaurantOwnerId);

        CreateRestaurantResponse response = restaurantManagementFacade.createRestaurant(
                createRestaurantRequest.toCreateRestaurantApplicationRequest(restaurantOwnerId));
        return ResponseEntity.created(URI.create("/restaurants/" + response.getRestaurantId()))
                .body(response);
    }

    @PatchMapping("/{restaurantId}")
    public ResponseEntity<UpdateRestaurantResponse> updateRestaurant(
            @PathVariable UUID restaurantId,
            @RequestBody UpdateRestaurantRequest updateRestaurantRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        UUID restaurantOwnerId = restaurantControllerHelper.getRestaurantOwnerId(authorizationHeader);

        log.info("레스토랑 정보 업데이트. restaurant Id : {}, ownerId : {}", restaurantId, restaurantOwnerId);

        return ResponseEntity.ok(restaurantManagementFacade.updateRestaurant(
                updateRestaurantRequest.toUpdateRestaurantApplicationRequest(), restaurantId, restaurantOwnerId));
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable UUID restaurantId,
                                                 @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        UUID restaurantOwnerId = restaurantControllerHelper.getRestaurantOwnerId(authorizationHeader);
        log.info("레스토랑 삭제. Restaurant Id : {}, Owner Id : {}", restaurantId, restaurantOwnerId);
        restaurantManagementFacade.deleteRestaurant(restaurantId, restaurantOwnerId);
        return ResponseEntity.noContent().build();
    }

}
