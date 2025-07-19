package com.orderingsystem.restaurant.domain.event;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class OrderApprovedEvent extends OrderApprovalEvent {

    private final DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher;

    public OrderApprovedEvent(OrderApproval orderApproval, UUID restaurantId, List<String> failureMessages,
                              ZonedDateTime createdAt,
                              DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher){
        super(orderApproval, restaurantId, failureMessages, createdAt);
        this.orderApprovedEventDomainEventPublisher=orderApprovedEventDomainEventPublisher;
    }

    @Override
    public void fire() {
        orderApprovedEventDomainEventPublisher.publish(this);
    }
}
