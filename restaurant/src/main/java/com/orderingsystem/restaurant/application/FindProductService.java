package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
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

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(UUID restaurantId, Pageable pageable) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        if (restaurant.isEmpty() || !restaurantProductPermissionCheckerService.canManageProduct(restaurant.get())) {
            throw new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
        }

        return productRepository.findByRestaurantId(restaurantId, pageable).map(ProductResponse::from);
    }

}
