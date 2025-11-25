package com.orderingsystem.order.domain.event;

import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderRejectedEvent extends OrderEvent {
    public OrderRejectedEvent(Order order, ZonedDateTime createdAt) {
        super(order, createdAt);
    }
}
