package com.orderingsystem.order.application.dto;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.order.domain.model.Product;
import com.orderingsystem.order.domain.model.Restaurant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantInfo {

    private UUID restaurantId;
    private boolean active;
    private List<ProductInfo> products;

    public Restaurant toRestaurant() {
        return Restaurant.builder()
                .restaurantId(this.restaurantId)
                .active(this.active)
                .products(products.stream().map(product ->
                                Product.builder()
                                        .productId(product.getProductId())
                                        .name(product.getName())
                                        .price(new Money(product.getPrice()))
                                        .available(product.isAvailable())
                                        .build())
                        .toList())
                .build();
    }
}
