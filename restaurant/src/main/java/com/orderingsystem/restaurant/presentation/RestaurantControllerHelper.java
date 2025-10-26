package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@RequiredArgsConstructor
@Component
public class RestaurantControllerHelper {

    private final CommonJwtUtil commonJwtUtil;

    protected UUID getRestaurantOwnerId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidCredentialsException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }
        return commonJwtUtil.getUserIdFromToken(authorizationHeader);
    }

    protected static void valid(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("잘못된 요청입니다.");
            throw new IllegalArgumentException(message);
        }
    }

}
