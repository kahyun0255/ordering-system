package com.orderingsystem.restaurant.domain.event;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class OrderRejectedEvent extends OrderApprovalEvent {

    private final DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher;

    public OrderRejectedEvent(OrderApproval orderApproval, UUID restaurantId, List<String> failureMessages,
                              ZonedDateTime createdAt,
                              DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher) {
        super(orderApproval, restaurantId, failureMessages, createdAt);
        this.orderRejectedEventDomainEventPublisher = orderRejectedEventDomainEventPublisher;
    }

    @Override
    public void fire() {
        orderRejectedEventDomainEventPublisher.publish(this);
    }
}
