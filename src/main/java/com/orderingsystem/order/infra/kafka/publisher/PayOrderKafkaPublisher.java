package com.orderingsystem.order.infra.kafka.publisher;

import com.orderingsystem.order.application.publisher.OrderPaidRestaurantRequestMassagePublisher;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import org.springframework.stereotype.Component;

@Component
public class PayOrderKafkaPublisher implements OrderPaidRestaurantRequestMassagePublisher {

    @Override
    public void publish(OrderPaidEvent domainEvent) {

    }
}
