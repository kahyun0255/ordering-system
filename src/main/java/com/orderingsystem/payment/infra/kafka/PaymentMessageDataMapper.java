package com.orderingsystem.payment.infra.kafka;

import com.orderingsystem.payment.domain.event.PaymentCompletedEvent;
import com.orderingsystem.payment.infra.kafka.message.PaymentResponseMessage;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageDataMapper {

    public PaymentResponseMessage paymentCompletedEventToPaymentResponseMessage(PaymentCompletedEvent domainEvent) {
        return PaymentResponseMessage.builder()
                .id(UUID.randomUUID())
                .paymentId(domainEvent.getPayment().getId())
                .orderId(domainEvent.getPayment().getOrderId())
                .customerId(domainEvent.getPayment().getCustomerId())
                .price(domainEvent.getPayment().getPrice().getAmount())
                .createdAt(domainEvent.getCreatedAt().toInstant())
                .paymentStatus(domainEvent.getPayment().getStatus().name())
                .failureMessages(domainEvent.getFailureMessages())
                .build();
    }
}
