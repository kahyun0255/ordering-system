package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import com.orderingsystem.restaurant.application.dto.request.UpdateProductApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
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
    private final DeleteProductService deleteProductService;
    private final UpdateProductService updateProductService;
    private final StockCachePort stockCachePort;

    public UUID create(CreateProductApplicationRequest request, UUID restaurantOwnerId, UUID restaurantId) {
        validateProductManagementPermission(restaurantOwnerId, restaurantId);
        UUID productId = createProductService.create(request, restaurantId, restaurantOwnerId);
        stockCachePort.update(productId, request.getQuantity());
        return productId;
    }

    public void delete(UUID restaurantId, UUID productId, UUID restaurantOwnerId) {
        validateProductManagementPermission(restaurantOwnerId, restaurantId);
        deleteProductService.deleteProduct(restaurantId, productId, restaurantOwnerId);
        stockCachePort.delete(productId);
    }

    public ProductResponse update(UUID ownerId, UUID restaurantId, UUID productId,
                                  UpdateProductApplicationRequest request) {
        validateProductManagementPermission(ownerId, restaurantId);
        return updateProductService.updateProduct(ownerId, restaurantId, productId, request);
    }

    private void validateProductManagementPermission(UUID restaurantOwnerId, UUID restaurantId) {
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);

        validateRestaurantOwnership(restaurantOwnerId, restaurantId);

        if (!restaurant.getStatus().canManageProduct()) {
            log.info("{} 상태의 {} 레스토랑은 {} 유저가 상품을 관리할 수 없습니다.", restaurant.getStatus(), restaurantId, restaurantOwnerId);
            throw new AccessDeniedException("현재 상태의 레스토랑에서는 상품을 관리할 수 없습니다.");
        }
    }

    private void validateRestaurantOwnership(UUID restaurantOwnerId, UUID restaurantId) {
        if (!restaurantAccessValidatorService.isRestaurantOwnership(restaurantOwnerId, restaurantId)) {
            log.info("{} 유저는 {} 레스토랑의 상품을 관리할 권한이 없습니다.", restaurantOwnerId, restaurantId);
            throw new AccessDeniedException("상품을 관리할 권한이 없습니다.");
        }
    }
}
