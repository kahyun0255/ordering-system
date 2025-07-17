package com.orderingsystem.payment.infra.kafka.publisher;

import com.orderingsystem.payment.application.publisher.PaymentCompleteMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentCompletedEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompleteKafkaPublisher implements PaymentCompleteMessagePublisher {
    @Override
    public void publish(PaymentCompletedEvent domainEvent) {
        //TODO : 결제 완료 Kafka 구현
    }
}
