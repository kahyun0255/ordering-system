package com.orderingsystem.payment.infra.kafka.publisher;

import com.orderingsystem.payment.application.publisher.PaymentFailedMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentFailedEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedKafkaPublisher implements PaymentFailedMessagePublisher {
    @Override
    public void publish(PaymentFailedEvent domainEvent) {
        //TODO : 결제 실패 Kafka 구현
    }
}
