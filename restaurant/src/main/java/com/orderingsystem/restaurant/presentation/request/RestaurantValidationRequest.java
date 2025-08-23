package com.orderingsystem.restaurant.presentation.request;

import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantValidationRequest {
    private UUID sagaId;
    private List<Item> items;
    private BigDecimal totalPrice;

    @Getter
    @Builder
    public static class Item {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
    }

    public RestaurantValidationApplicationRequest toRestaurantValidationApplicationRequest() {
        return RestaurantValidationApplicationRequest.builder()
                .sagaId(this.sagaId)
                .items(this.items.stream().map(item ->
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build()).toList())
                .totalPrice(this.totalPrice)
                .build();
    }
}
