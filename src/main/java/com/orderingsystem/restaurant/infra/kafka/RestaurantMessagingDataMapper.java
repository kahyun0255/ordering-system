package com.orderingsystem.restaurant.infra.kafka;

import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalResponseMessage;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMessagingDataMapper {

    public RestaurantApprovalResponseMessage orderApprovalEventToRestaurantApprovalResponseMessage(
            OrderApprovalEvent domainEvent) {
        return RestaurantApprovalResponseMessage.builder()
                .id(UUID.randomUUID())
                .orderId(domainEvent.getOrderApproval().getOrderId())
                .restaurantId(domainEvent.getRestaurantId())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .orderApprovalStatus(domainEvent.getOrderApproval().getStatus())
                .failureMessages(domainEvent.getFailureMessages())
                .build();
    }
}
