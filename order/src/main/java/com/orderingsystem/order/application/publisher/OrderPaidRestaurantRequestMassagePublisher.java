package com.orderingsystem.order.application.publisher;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.order.domain.event.OrderPaidEvent;

public interface OrderPaidRestaurantRequestMassagePublisher extends DomainEventPublisher<OrderPaidEvent> {
}
