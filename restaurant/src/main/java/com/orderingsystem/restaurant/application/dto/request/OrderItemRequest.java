package com.orderingsystem.restaurant.application.dto.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderItemRequest {

    private UUID productId;
    private int quantity;

}
