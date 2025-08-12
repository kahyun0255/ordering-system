package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.restaurant.application.RestaurantManagementService;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.presentation.request.CreateRestaurantRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
@Slf4j
public class RestaurantManagementController {

    private final CommonJwtUtil commonJwtUtil;
    private final RestaurantManagementService restaurantManagementService;

    @PostMapping
    public ResponseEntity<CreateRestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest createRestaurantRequest, BindingResult bindingResult,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        valid(bindingResult);
        UUID restaurantOwnerId = getRestaurantOwnerId(authorizationHeader);

        log.info("레스토랑 생성. Restaurant Owner Id : {}", restaurantOwnerId);

        return ResponseEntity.ok(restaurantManagementService.createRestaurant(
                createRestaurantRequest.toCreateRestaurantApplicationRequest(restaurantOwnerId)));
    }

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
        if (authorizationHeader == null || authorizationHeader.isBlank()){
            throw new InvalidCredentialsException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }
        return commonJwtUtil.getUserIdFromToken(authorizationHeader);
    }

}
