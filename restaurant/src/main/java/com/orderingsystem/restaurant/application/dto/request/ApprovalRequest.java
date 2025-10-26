package com.orderingsystem.restaurant.application.dto.request;

import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ApprovalRequest {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private RestaurantOrderStatus restaurantOrderStatus;
    private List<OrderItemRequest> products;
    private BigDecimal price;
    private Instant createdAt;

}
