package com.orderingsystem.order.domain.event;

import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderCancelledEvent extends OrderEvent {

    public OrderCancelledEvent(Order order, ZonedDateTime createdAt) {
        super(order, createdAt);
    }

}
