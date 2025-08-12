package com.orderingsystem.restaurant.application.mapper;

import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.restaurant.application.outbox.order.model.OrderEventPayload;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.model.RestaurantUpdateEventPayload;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
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

    public RestaurantUpdateEventPayload createdRestaurantEventToRestaurantUpdateEventPayload(
            CreatedRestaurantEvent createdRestaurantEvent) {
        return RestaurantUpdateEventPayload.builder()
                .restaurantId(createdRestaurantEvent.getRestaurant().getRestaurantId().toString())
                .name(createdRestaurantEvent.getRestaurant().getName())
                .active(createdRestaurantEvent.getRestaurant().getActive())
                .createdAt(createdRestaurantEvent.getCreatedAt())
                .type(OutboxEventOperation.INSERT.name())
                .build();
    }

}
