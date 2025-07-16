package com.orderingsystem.order.infra.kafka;

import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import org.springframework.stereotype.Component;

@Component
public class CreateOrderKafkaPublisher implements OrderCreatedPaymentRequestMessagePublisher {

    @Override
    public void publish(OrderCreateEvent domainEvent) {
        //TODO: Kafka 구현
    }
}
