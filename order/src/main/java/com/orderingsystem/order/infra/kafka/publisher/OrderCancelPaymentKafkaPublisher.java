package com.orderingsystem.order.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.order.application.publisher.OrderCancelledPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.infra.kafka.OrderMessageConfigData;
import com.orderingsystem.order.infra.kafka.OrderMessagingDataMapper;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelPaymentKafkaPublisher implements OrderCancelledPaymentRequestMessagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderMessageConfigData orderMessageConfigData;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OrderCancelledEvent domainEvent) {

    }
}
