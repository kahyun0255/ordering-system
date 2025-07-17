package com.orderingsystem.order.infra.kafka;

import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderMessagingDataMapper {

    public PaymentRequestMessage toPaymentRequestMessage(OrderCreateEvent domainEvent) {
        return PaymentRequestMessage.builder()
                .id(UUID.randomUUID())
                .customerId(domainEvent.getOrder().getCustomerId())
                .orderId(domainEvent.getOrder().getId())
                .price(domainEvent.getOrder().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();
    }
}
