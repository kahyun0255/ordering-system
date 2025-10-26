package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.restaurant.application.dto.request.OrderItemRequest;
import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalRequestMessage {

    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private RestaurantOrderStatus restaurantOrderStatus;
    private List<OrderItemMessage> products;
    private BigDecimal price;
    private Instant createdAt;

    public ApprovalRequest toApprovalRequest(UUID id) {
        return ApprovalRequest.builder()
                .id(id)
                .sagaId(this.sagaId)
                .orderId(this.orderId)
                .restaurantId(this.restaurantId)
                .restaurantOrderStatus(this.restaurantOrderStatus)
                .products(this.products.stream().map(product ->
                        OrderItemRequest.builder()
                                .productId(product.getId())
                                .quantity(product.getQuantity())
                                .build()).toList())
                .price(this.price)
                .createdAt(this.createdAt)
                .build();
    }
}
