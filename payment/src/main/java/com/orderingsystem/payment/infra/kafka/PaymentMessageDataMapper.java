package com.orderingsystem.payment.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.infra.kafka.message.PaymentResponseMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMessageDataMapper {

    private final ObjectMapper objectMapper;

    public <T> T getOrderPaymentEventPayload(String payload, Class<T> outputType) {
        try {
            return objectMapper.readValue(payload, outputType);
        } catch (JsonProcessingException e) {
            log.error("Could not read {} object", outputType.getName(), e);
            throw new RuntimeException("Could not read " + outputType.getName() + " object", e);
        }
    }

    public PaymentResponseMessage orderEventPayloadToPaymentResponseMessage(UUID sagaId,
                                                                            OrderEventPayload orderEventPayload) {
        return PaymentResponseMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentId(UUID.fromString(orderEventPayload.getPaymentId()))
                .customerId(UUID.fromString(orderEventPayload.getCustomerId()))
                .orderId(UUID.fromString(orderEventPayload.getOrderId()))
                .price(orderEventPayload.getPrice())
                .createdAt(orderEventPayload.getCreatedAt().toInstant())
                .paymentStatus(orderEventPayload.getPaymentStatus())
                .failureMessages(orderEventPayload.getFailureMessages())
                .build();
    }
}
