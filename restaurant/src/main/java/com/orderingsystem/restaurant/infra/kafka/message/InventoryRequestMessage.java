package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.restaurant.application.dto.request.OrderItemRequest;
import com.orderingsystem.restaurant.application.dto.request.ProductRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class InventoryRequestMessage {

    private UUID orderId;
    private UUID sagaId;
    private UUID restaurantId;
    private Instant createdAt;
    private String type;
    private List<String> failureMessage;
    private List<OrderItemMessage> products;

    public ProductRequest toProductRequest(UUID id){
        return ProductRequest.builder()
                .id(id)
                .orderId(this.orderId)
                .sagaId(this.sagaId)
                .restaurantId(this.restaurantId)
                .createdAt(this.createdAt)
                .type(this.type)
                .failureMessage(this.failureMessage)
                .products(this.products.stream().map(product ->
                        OrderItemRequest.builder()
                                .productId(product.getId())
                                .quantity(product.getQuantity())
                                .build()).toList())
                .build();
    }

}
