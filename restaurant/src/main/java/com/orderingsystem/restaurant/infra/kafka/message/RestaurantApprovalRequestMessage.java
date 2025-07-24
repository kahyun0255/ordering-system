package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.restaurant.application.dto.request.ApprovalOrderItem;
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

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private RestaurantOrderStatus restaurantOrderStatus;
    private List<RestaurantApprovalOrderItem> products;
    private BigDecimal price;
    private Instant createdAt;

    public ApprovalRequest toApprovalRequest() {
        return ApprovalRequest.builder()
                .id(this.id)
                .sagaId(this.sagaId)
                .orderId(this.orderId)
                .restaurantId(this.restaurantId)
                .restaurantOrderStatus(this.restaurantOrderStatus)
                .products(this.products.stream().map(product ->
                        ApprovalOrderItem.builder()
                                .productId(product.getProductId())
                                .quantity(product.getQuantity())
                                .build()).toList())
                .price(this.price)
                .createdAt(this.createdAt)
                .build();
    }
}
