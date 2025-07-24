package com.orderingsystem.restaurant.application.mapper;

import com.orderingsystem.restaurant.application.outbox.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDataMapper {

    public OrderEventPayload restaurantApprovalEventToOrderEventPayload(OrderApprovalEvent orderApprovalEvent) {
        return OrderEventPayload.builder()
                .orderId(orderApprovalEvent.getOrderApproval().getOrderId().toString())
                .restaurantId(orderApprovalEvent.getRestaurantId().toString())
                .createdAt(orderApprovalEvent.getCreatedAt())
                .orderApprovalStatus(orderApprovalEvent.getOrderApproval().getStatus().name())
                .failureMessages(orderApprovalEvent.getFailureMessages())
                .build();
    }

}
