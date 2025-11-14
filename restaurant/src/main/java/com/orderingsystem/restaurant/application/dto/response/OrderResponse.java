package com.orderingsystem.restaurant.application.dto.response;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private UUID id;
    private UUID orderId;
    private OrderApprovalStatus status;
    private LocalDateTime createdAt;

    public static OrderResponse from(OrderApproval orderApproval){
        return OrderResponse.builder()
                .id(orderApproval.getId())
                .orderId(orderApproval.getOrderId())
                .status(orderApproval.getStatus())
                .createdAt(orderApproval.getCreatedAt())
                .build();
    }

}
