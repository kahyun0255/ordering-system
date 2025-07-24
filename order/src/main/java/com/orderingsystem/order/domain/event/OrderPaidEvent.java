package com.orderingsystem.order.domain.event;

import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderPaidEvent extends OrderEvent {

    public OrderPaidEvent(Order order, ZonedDateTime createdAt) {
        super(order, createdAt);
    }

}
