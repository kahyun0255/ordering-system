package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalResponseMessage {

    private UUID id;
    private UUID orderId;
    private UUID restaurantId;
    private Instant createdAt;
    private OrderApprovalStatus orderApprovalStatus;
    private List<String> failureMessages;

    public RestaurantApprovalResponse toRestaurantApprovalResponse(){
        return RestaurantApprovalResponse.builder()
                .id(this.id)
                .orderId(this.orderId)
                .restaurantId(this.restaurantId)
                .createdAt(this.createdAt)
                .orderApprovalStatus(this.orderApprovalStatus)
                .failureMessages(this.failureMessages)
                .build();
    }
}
