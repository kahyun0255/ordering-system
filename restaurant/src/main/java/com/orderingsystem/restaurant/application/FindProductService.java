package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.exception.ProductNotFoundException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantProductPermissionCheckerService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindProductService {

    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantProductPermissionCheckerService restaurantProductPermissionCheckerService;
    private final RestaurantProductRepository restaurantProductRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(UUID restaurantId, Pageable pageable) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        if (restaurant.isEmpty() || !restaurantProductPermissionCheckerService.canManageProduct(restaurant.get())) {
            throw new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
        }

        return productRepository.findByRestaurantId(restaurantId, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findOne(UUID restaurantId, UUID productId) {
        Restaurant restaurant = findValidRestaurant(restaurantId);
        validateRestaurantAccessPermission(restaurant);
        Product product = findAvailableProduct(productId);
        validateRestaurantProductMapping(restaurantId, productId);

        return ProductResponse.from(product);
    }

    private Restaurant findValidRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .filter(r -> !r.getStatus().equals(RestaurantStatus.DELETED))
                .orElseThrow(() -> new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다."));
    }

    private void validateRestaurantAccessPermission(Restaurant restaurant) {
        if (!restaurantProductPermissionCheckerService.canManageProduct(restaurant)) {
            throw new AccessDeniedException("물품을 조회할 권한이 없습니다.");
        }
    }

    private Product findAvailableProduct(UUID productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품 정보를 찾을 수 없습니다"));

        if (!product.isAvailable()) {
            throw new AccessDeniedException("현재 판매하고 있지 않은 상품입니다.");
        }

        return product;
    }

    private void validateRestaurantProductMapping(UUID restaurantId, UUID productId) {
        restaurantProductRepository.findByRestaurantIdAndProductId(restaurantId, productId)
                .orElseThrow(() -> new ProductNotFoundException("해당 레스토랑이 판매하고 있지 않은 상품입니다."));
    }

}
