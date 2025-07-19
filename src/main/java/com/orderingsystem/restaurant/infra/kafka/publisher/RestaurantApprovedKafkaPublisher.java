package com.orderingsystem.restaurant.infra.kafka.publisher;

import com.orderingsystem.restaurant.application.publisher.OrderApprovedMessagePublisher;
import com.orderingsystem.restaurant.domain.event.OrderApprovedEvent;
import org.springframework.stereotype.Component;

@Component
public class RestaurantApprovedKafkaPublisher implements OrderApprovedMessagePublisher {
    @Override
    public void publish(OrderApprovedEvent domainEvent) {

    }
}
