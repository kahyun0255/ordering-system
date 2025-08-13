package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
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
import org.springframework.web.bind.annotation.GetMapping;
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

    private final CommonJwtUtil commonJwtUtil;
    private final RestaurantManagementFacade restaurantManagementFacade;

    @PostMapping
    public ResponseEntity<CreateRestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest createRestaurantRequest, BindingResult bindingResult,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        valid(bindingResult);
        UUID restaurantOwnerId = getRestaurantOwnerId(authorizationHeader);

        log.info("레스토랑 생성. Restaurant Owner Id : {}", restaurantOwnerId);

        CreateRestaurantResponse response = restaurantManagementFacade.createRestaurant(
                createRestaurantRequest.toCreateRestaurantApplicationRequest(restaurantOwnerId));
        return ResponseEntity.created(URI.create("/restaurants/" + response.getRestaurantId()))
                .body(response);
    }

    @PatchMapping("/{restaurantId}")
    public ResponseEntity<UpdateRestaurantResponse> updateRestaurant(
            @PathVariable String restaurantId,
            @RequestBody UpdateRestaurantRequest updateRestaurantRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        UUID restaurantOwnerId = getRestaurantOwnerId(authorizationHeader);

        log.info("레스토랑 정보 업데이트. restaurant Id : {}", restaurantId);

        return ResponseEntity.ok(restaurantManagementFacade.updateRestaurant(
                updateRestaurantRequest.toUpdateRestaurantApplicationRequest(), restaurantId, restaurantOwnerId));
    }

    @GetMapping("/re")

    private static void valid(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("잘못된 요청입니다.");
            throw new IllegalArgumentException(message);
        }
    }

    private UUID getRestaurantOwnerId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidCredentialsException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }
        return commonJwtUtil.getUserIdFromToken(authorizationHeader);
    }

}
