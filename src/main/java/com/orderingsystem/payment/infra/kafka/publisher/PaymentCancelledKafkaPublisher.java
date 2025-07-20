package com.orderingsystem.payment.infra.kafka.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.infrastructure.kafka.KafkaMessageHelper;
import com.orderingsystem.infrastructure.kafka.KafkaProducer;
import com.orderingsystem.payment.application.publisher.PaymentCancelledMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentCancelledEvent;
import com.orderingsystem.payment.infra.kafka.PaymentMessageConfigData;
import com.orderingsystem.payment.infra.kafka.PaymentMessageDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCancelledKafkaPublisher implements PaymentCancelledMessagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final ObjectMapper objectMapper;
    private final PaymentMessageConfigData paymentMessageConfigData;
    private final PaymentMessageDataMapper paymentMessageDataMapper;

    @Override
    public void publish(PaymentCancelledEvent domainEvent) {

    }
}
