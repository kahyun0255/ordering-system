package com.orderingsystem.order.domain.event;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderCreateEvent extends OrderEvent {

    public OrderCreateEvent(Order order, ZonedDateTime createdAt) {
        super(order, createdAt);
    }

    @Override
    public void fire() {

    }
}
