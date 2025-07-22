package com.orderingsystem.order.application.publisher;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.order.domain.event.OrderCreateEvent;

public interface OrderCreatedPaymentRequestMessagePublisher extends DomainEventPublisher<OrderCreateEvent> {
}
