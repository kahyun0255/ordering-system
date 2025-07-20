package com.orderingsystem.order.infra.kafka.publisher;

import com.orderingsystem.order.application.publisher.OrderCancelledPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelPaymentKafkaPublisher implements OrderCancelledPaymentRequestMessagePublisher {
    @Override
    public void publish(OrderCancelledEvent domainEvent) {

    }
}
