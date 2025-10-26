package com.orderingsystem.restaurant.application.dto.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProductRequest {

    private UUID id;
    private UUID orderId;
    private UUID sagaId;
    private UUID restaurantId;
    private Instant createdAt;
    private String type;
    private List<String> failureMessage;
    private List<OrderItemRequest> products;

}
