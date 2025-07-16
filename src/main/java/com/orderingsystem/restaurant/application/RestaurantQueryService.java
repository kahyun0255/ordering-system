package com.orderingsystem.restaurant.application;

import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.domain.model.Product;
import com.orderingsystem.order.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.application.dto.response.ProductInfoResponse;
import com.orderingsystem.restaurant.application.dto.response.RestaurantInfoResponse;
import com.orderingsystem.restaurant.application.exception.RestaurantApplicationException;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantQueryService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantInfoResponse getRestaurantInfo(UUID restaurantId, List<UUID> productIds) {
        List<RestaurantInfoView> restaurantInfo =
                restaurantRepository.findRestaurantInfo(restaurantId, productIds);

        if (restaurantInfo.isEmpty()) {
            throw new RestaurantApplicationException("레스토랑 정보를 찾을 수 없습니다.");
        }

        Restaurant restaurant = restaurantEntityToRestaurant(restaurantInfo);

        return RestaurantInfoResponse.builder()
                .restaurantId(restaurant.getRestaurantId())
                .products(restaurant.getProducts().stream().map(product->new ProductInfoResponse(
                        product.getProductId(),
                        product.getName(),
                        product.getPrice().getAmount())
                ).toList())
                .active(restaurant.isActive())
                .build();
    }

    private Restaurant restaurantEntityToRestaurant(List<RestaurantInfoView> restaurantInfo) {
        RestaurantInfoView restaurantEntity = restaurantInfo.stream().findFirst()
                .orElseThrow(() -> new OrderApplicationException("레스토랑을 찾을 수 없습니다."));

        List<Product> restaurantProducts = restaurantInfo.stream().map(entity ->
                        new Product(entity.getProductId(),
                                entity.getProductName(),
                                entity.getProductPrice()))
                .toList();

        return Restaurant.builder()
                .restaurantId(restaurantEntity.getRestaurantId())
                .products(restaurantProducts)
                .active(restaurantEntity.getRestaurantActive())
                .build();
    }
}
