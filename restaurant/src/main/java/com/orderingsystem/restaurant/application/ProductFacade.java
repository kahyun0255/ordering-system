package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {

    private final RestaurantAccessValidatorService restaurantAccessValidatorService;
    private final CreateProductService createProductService;

    public UUID create(CreateProductApplicationRequest request, UUID restaurantOwnerId, UUID restaurantId) {
        if (!restaurantAccessValidatorService.isRestaurantOwnership(restaurantOwnerId, restaurantId)) {
            log.info("{} 유저는 {} 레스토랑의 상품을 생성할 권한이 없습니다.", restaurantOwnerId, restaurantId);
            throw new AccessDeniedException("상품을 생성할 권한이 없습니다.");
        }
        validateProductManagementPermission(restaurantOwnerId, restaurantId);

        return createProductService.create(request, restaurantId, restaurantOwnerId);
    }

    private void validateProductManagementPermission(UUID requestOwnerId, UUID restaurantId) {
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        if (!restaurant.getStatus().canManageProduct()) {
            log.info("{} 상태의 {} 레스토랑은 {} 유저가 상품을 관리할 수 없습니다.", restaurant.getStatus(), restaurantId, requestOwnerId);
            throw new AccessDeniedException("상품을 관리할 권한이 없습니다.");
        }
    }

}
