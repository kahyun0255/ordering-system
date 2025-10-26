package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateProductService {

    private final ProductRepository productRepository;
    private final RestaurantProductRepository restaurantProductRepository;

    public UUID create(CreateProductApplicationRequest request, UUID restaurantId, UUID restaurantOwnerId) {
        Product product = createProduct(request);
        registerProductToRestaurant(restaurantId, product);

        log.info("{} 유저가 {} 레스토랑에 {} 상품 추가.", restaurantOwnerId, restaurantId, product.getProductId());

        return product.getProductId();
    }

    private static Product createProduct(CreateProductApplicationRequest request) {
        return Product.create(request.getName(), request.getAvailable(), request.getPrice(), request.getQuantity());
    }

    private void registerProductToRestaurant(UUID restaurantId, Product product) {
        productRepository.save(product);
        restaurantProductRepository.save(RestaurantProduct.builder()
                .restaurantId(restaurantId)
                .productId(product.getProductId())
                .build());
    }

}
