package com.orderingsystem.restaurant.application.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantValidationApplicationRequest {
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
}
