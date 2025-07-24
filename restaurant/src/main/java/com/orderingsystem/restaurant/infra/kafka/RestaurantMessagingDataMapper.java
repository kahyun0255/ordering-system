package com.orderingsystem.restaurant.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.outbox.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalResponseMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantMessagingDataMapper {

    private final ObjectMapper objectMapper;

    public <T> T getOrderEventPayload(String payload, Class<T> outputType) {
        try {
            return objectMapper.readValue(payload, outputType);
        } catch (JsonProcessingException e) {
            log.error("Could not read {} object", outputType.getName(), e);
            throw new RuntimeException("Could not read " + outputType.getName() + " object", e);
        }
    }

    public RestaurantApprovalResponseMessage orderEventPayloadToRestaurantApprovalResponseMessage(
            OrderEventPayload orderEventPayload, UUID sagaId) {
        return RestaurantApprovalResponseMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(UUID.fromString(orderEventPayload.getOrderId()))
                .restaurantId(UUID.fromString(orderEventPayload.getRestaurantId()))
                .createdAt(orderEventPayload.getCreatedAt().toInstant())
                .orderApprovalStatus(OrderApprovalStatus.valueOf(orderEventPayload.getOrderApprovalStatus()))
                .failureMessages(orderEventPayload.getFailureMessages())
                .build();
    }
}
