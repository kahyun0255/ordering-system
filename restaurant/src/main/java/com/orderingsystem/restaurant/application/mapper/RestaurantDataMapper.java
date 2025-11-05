package com.orderingsystem.restaurant.application.mapper;

import com.orderingsystem.restaurant.application.outbox.order.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.event.orderaccept.OrderAcceptEvent;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovalEvent;
import org.springframework.stereotype.Component;

@Component
public class RestaurantDataMapper {

    public OrderEventPayload restaurantApprovalEventToOrderEventPayload(OrderApprovalEvent orderApprovalEvent) {
        return OrderEventPayload.builder()
                .orderId(orderApprovalEvent.getOrderApproval().getOrderId().toString())
                .restaurantId(orderApprovalEvent.getRestaurantId().toString())
                .createdAt(orderApprovalEvent.getCreatedAt())
                .orderApprovalStatus(orderApprovalEvent.getOrderApproval().getStatus().name())
                .failureMessages(null)
                .build();
    }

    public OrderEventPayload restaurantAcceptEventToOrderEventPayload(OrderAcceptEvent orderAcceptEvent) {
        return OrderEventPayload.builder()
                .orderId(orderAcceptEvent.getOrderApproval().getOrderId().toString())
                .restaurantId(orderAcceptEvent.getRestaurantId().toString())
                .createdAt(orderAcceptEvent.getCreatedAt())
                .orderApprovalStatus(orderAcceptEvent.getOrderApproval().getStatus().name())
                .failureMessages(orderAcceptEvent.getFailureMessages())
                .build();
    }

}
