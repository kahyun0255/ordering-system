package com.orderingsystem.restaurant.domain.event.order.orderapproval;

import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OrderRejectedEvent extends OrderApprovalEvent {

    public OrderRejectedEvent(OrderApproval orderApproval, UUID restaurantId, ZonedDateTime createdAt) {
        super(orderApproval, restaurantId, createdAt);
    }

}
