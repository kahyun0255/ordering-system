package com.orderingsystem.restaurant.application.dto.response;

import com.orderingsystem.restaurant.domain.model.Product;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponse {

    private UUID productId;
    private String name;
    private BigDecimal price;
    private boolean available;
    private int quantity;

    public static ProductResponse from(Product product){
        return ProductResponse.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .price(product.getPrice().getAmount())
                .available(product.isAvailable())
                .quantity(product.getQuantity())
                .build();
    }

}
