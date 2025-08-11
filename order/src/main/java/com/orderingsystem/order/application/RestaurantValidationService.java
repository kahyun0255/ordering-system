package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.ProductInfo;
import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.model.restaurant.RestaurantInfoView;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantReadRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantValidationService {
    private final RestaurantReadRepository restaurantReadRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public RestaurantInfo getRestaurantInfo(UUID restaurantId, List<UUID> productIds) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
        if (restaurant.isEmpty()) {
            log.error("레스토랑 정보를 찾을 수 없습니다. Restaurant Id : {} ", restaurantId);
            throw new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
        }

        List<RestaurantInfoView> restaurantInfo = restaurantReadRepository.findRestaurantInfo(restaurantId, productIds);
        if (restaurantInfo.isEmpty()) {
            log.error("상품 정보를 찾을 수 없습니다. productId : {}, restaurantId : {}", productIds, restaurantId);
            throw new RestaurantNotFoundException("상품 정보를 찾을 수 없습니다.");
        }

        Set<UUID> findIds = restaurantInfo.stream()
                .map(RestaurantInfoView::getProductId)
                .collect(Collectors.toSet());

        if (!findIds.containsAll(productIds)) {
            log.error("요청한 상품 중 일부를 찾을 수 없습니다. RestaurantId : {}, productIds : {}, findIds : {}", restaurantId,
                    productIds, findIds);
            throw new RestaurantNotFoundException("요청한 상품 중 일부를 찾을 수 없습니다.");
        }

        return RestaurantInfo.builder()
                .restaurantId(restaurantInfo.get(0).getRestaurantId())
                .restaurantName(restaurantInfo.get(0).getRestaurantName())
                .active(restaurantInfo.get(0).getRestaurantActive())
                .products(restaurantInfo.stream().map(p ->
                        ProductInfo.builder()
                                .productId(p.getProductId())
                                .name(p.getProductName())
                                .price(p.getProductPrice())
                                .available(p.getProductAvailable())
                                .build()
                ).toList())
                .build();
    }
}
