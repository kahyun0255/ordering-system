package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalRequestMessage {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private RestaurantOrderStatus restaurantOrderStatus;
    private List<RestaurantApprovalOrderItem> products;
    private BigDecimal price;
    private Instant createdAt;

}
