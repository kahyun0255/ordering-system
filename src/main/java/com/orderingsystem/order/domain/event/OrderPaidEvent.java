package com.orderingsystem.order.domain.event;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;

public class OrderPaidEvent extends OrderEvent {

    private final DomainEventPublisher<OrderPaidEvent> orderPaidEventPublisher;

    public OrderPaidEvent(Order order, ZonedDateTime createdAt,
                          DomainEventPublisher<OrderPaidEvent> orderPaidEventPublisher) {
        super(order, createdAt);
        this.orderPaidEventPublisher = orderPaidEventPublisher;
    }

    @Override
    public void fire() {
        orderPaidEventPublisher.publish(this);
    }
}
