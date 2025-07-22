package com.orderingsystem.order.domain.event;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;
import lombok.Getter;

@Getter
public abstract class OrderEvent implements DomainEvent<Order> {

    private final Order order;
    private final ZonedDateTime createdAt;

    public OrderEvent(Order order, ZonedDateTime createdAt) {
        this.order = order;
        this.createdAt = createdAt;
    }

}
