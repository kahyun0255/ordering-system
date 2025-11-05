package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantOrderDecisionMessage {

    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private Instant createdAt;
    private OrderApprovalStatus orderApprovalStatus;
    private List<String> failureMessages;

    public RestaurantOrderDecisionResponse toDecisionResponse(UUID id){
        return RestaurantOrderDecisionResponse.builder()
                .id(id)
                .sagaId(this.sagaId)
                .orderId(this.orderId)
                .restaurantId(this.restaurantId)
                .createdAt(this.createdAt)
                .orderApprovalStatus(this.orderApprovalStatus)
                .failureMessages(this.failureMessages)
                .build();
    }
}
