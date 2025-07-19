package com.orderingsystem.restaurant.infra.kafka.publisher;

import com.orderingsystem.restaurant.application.publisher.OrderRejectedMessagePublisher;
import com.orderingsystem.restaurant.domain.event.OrderRejectedEvent;
import org.springframework.stereotype.Component;

@Component
public class RestaurantRejectedKafkaPublisher implements OrderRejectedMessagePublisher {
    @Override
    public void publish(OrderRejectedEvent domainEvent) {

    }
}
