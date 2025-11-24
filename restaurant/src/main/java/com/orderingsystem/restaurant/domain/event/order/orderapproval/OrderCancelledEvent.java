package com.orderingsystem.restaurant.domain.event.order.orderapproval;

import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OrderCancelledEvent extends OrderApprovedEvent {
    public OrderCancelledEvent(OrderApproval orderApproval,
                               UUID restaurantId, ZonedDateTime createdAt) {
        super(orderApproval, restaurantId, createdAt);
    }
}
