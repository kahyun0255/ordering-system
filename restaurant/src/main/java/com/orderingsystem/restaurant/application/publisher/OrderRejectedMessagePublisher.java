package com.orderingsystem.restaurant.application.publisher;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.restaurant.domain.event.OrderRejectedEvent;

public interface OrderRejectedMessagePublisher extends DomainEventPublisher<OrderRejectedEvent> {
}
