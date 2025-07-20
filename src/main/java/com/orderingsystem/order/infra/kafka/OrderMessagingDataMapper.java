package com.orderingsystem.order.infra.kafka;

import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalOrderItem;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalRequestMessage;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderMessagingDataMapper {

    public PaymentRequestMessage orderCreateEventToPaymentRequestMessage(OrderCreateEvent domainEvent) {
        return PaymentRequestMessage.builder()
                .id(UUID.randomUUID())
                .customerId(domainEvent.getOrder().getCustomerId())
                .orderId(domainEvent.getOrder().getId())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();
    }

    public PaymentRequestMessage orderCancelledEventToPaymentRequestMessage(OrderCancelledEvent domainEvent) {
        return PaymentRequestMessage.builder()
                .id(UUID.randomUUID())
                .customerId(domainEvent.getOrder().getCustomerId())
                .orderId(domainEvent.getOrder().getId())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED)
                .build();
    }

    public RestaurantApprovalRequestMessage orderPaidEventToRestaurantApprovalRequestMessage(
            OrderPaidEvent domainEvent) {
        return RestaurantApprovalRequestMessage.builder()
                .id(UUID.randomUUID())
                .orderId(domainEvent.getOrder().getId())
                .restaurantId(domainEvent.getOrder().getRestaurantId())
                .restaurantOrderStatus(RestaurantOrderStatus.PAID)
                .products(domainEvent.getOrder().getItems().stream().map(item ->
                        RestaurantApprovalOrderItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .build();
    }
}
