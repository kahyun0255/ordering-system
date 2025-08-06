package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.RestaurantDTO;
import com.orderingsystem.restaurant.application.dto.response.ProductInfoResponse;
import com.orderingsystem.restaurant.application.dto.response.RestaurantInfoResponse;
import com.orderingsystem.restaurant.application.exception.RestaurantApplicationException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
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

        RestaurantDTO restaurant = restaurantEntityToRestaurant(restaurantInfo);

        return RestaurantInfoResponse.builder()
                .restaurantId(restaurant.getRestaurantId())
                .products(restaurant.getProducts().stream().map(product->new ProductInfoResponse(
                        product.getProductId(),
                        product.getName(),
                        product.getPrice().getAmount(),
                        product.isAvailable())
                ).toList())
                .active(restaurant.isActive())
                .build();
    }

    private RestaurantDTO restaurantEntityToRestaurant(List<RestaurantInfoView> restaurantInfo) {
        RestaurantInfoView restaurantEntity = restaurantInfo.stream().findFirst()
                .orElseThrow(() -> new RestaurantApplicationException("레스토랑을 찾을 수 없습니다."));

        List<Product> restaurantProducts = restaurantInfo.stream().map(restaurant ->
                        Product.builder()
                                .productId(restaurant.getProductId())
                                .name(restaurant.getProductName())
                                .price(restaurant.getProductPrice())
                                .available(restaurant.getProductAvailable())
                                .build())
                .toList();

        return RestaurantDTO.builder()
                .restaurantId(restaurantEntity.getRestaurantId())
                .products(restaurantProducts)
                .active(restaurantEntity.getRestaurantActive())
                .build();
    }
}
