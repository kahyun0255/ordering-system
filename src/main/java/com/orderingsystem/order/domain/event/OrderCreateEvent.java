package com.orderingsystem.order.domain.event;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderCreateEvent extends OrderEvent {

    private final DomainEventPublisher<OrderCreateEvent> orderCreateEventPublisher;

    public OrderCreateEvent(Order order, ZonedDateTime createdAt,
                            DomainEventPublisher<OrderCreateEvent> orderCreateEventPublisher) {
        super(order, createdAt);
        this.orderCreateEventPublisher = orderCreateEventPublisher;
    }

    @Override
    public void fire() {
        orderCreateEventPublisher.publish(this);
    }
}
