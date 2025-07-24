package com.orderingsystem.order.infra.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventPayload;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalOrderItem;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalRequestMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderMessagingDataMapper {

    private final ObjectMapper objectMapper;

    public <T> T getEventPayload(String payload, Class<T> outputType) {
        try {
            return objectMapper.readValue(payload, outputType);
        } catch (JsonMappingException e) {
            log.error("{} 객체 매핑에 실패했습니다. 원인: {}", outputType.getName(), e.getMessage());
            throw new OrderDomainException(outputType.getName() + " 객체 매핑에 실패했습니다.", e);
        } catch (JsonProcessingException e) {
            log.error("{} 객체 처리 중 오류가 발생했습니다. 원인: {}", outputType.getName(), e.getMessage());
            throw new OrderDomainException(outputType.getName() + " 객체 처리 중 오류가 발생했습니다.", e);
        }
    }

    public PaymentRequestMessage orderPaymentEventToPaymentRequestMessage(UUID sagaId,
                                                                          OrderPaymentEventPayload orderPaymentEventPayload) {
        return PaymentRequestMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .customerId(UUID.fromString(orderPaymentEventPayload.getCustomerId()))
                .orderId(UUID.fromString(orderPaymentEventPayload.getOrderId()))
                .price(orderPaymentEventPayload.getPrice())
                .createdAt(orderPaymentEventPayload.getCreatedAt().toInstant())
                .paymentOrderStatus(PaymentOrderStatus.valueOf(orderPaymentEventPayload.getPaymentOrderStatus()))
                .build();
    }

    public RestaurantApprovalRequestMessage restaurantApprovalEventToRestaurantApprovalRequestMessage
            (UUID sagaId, RestaurantApprovalEventPayload restaurantApprovalEventPayload) {
        return RestaurantApprovalRequestMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(UUID.fromString(restaurantApprovalEventPayload.getOrderId()))
                .restaurantId(UUID.fromString(restaurantApprovalEventPayload.getRestaurantId()))
                .restaurantOrderStatus(RestaurantOrderStatus.valueOf(restaurantApprovalEventPayload.getRestaurantOrderStatus()))
                .products(restaurantApprovalEventPayload.getProducts().stream().map(product ->
                        RestaurantApprovalOrderItem.builder()
                                .productId(UUID.fromString(product.getId()))
                                .quantity(product.getQuantity())
                                .build()).toList())
                .price(restaurantApprovalEventPayload.getPrice())
                .createdAt(restaurantApprovalEventPayload.getCreatedAt().toInstant())
                .build();
    }
}
