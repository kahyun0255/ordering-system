package com.orderingsystem.payment.application.mapper;

import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import org.springframework.stereotype.Component;

@Component
public class PaymentDataMapper {

    public OrderEventPayload paymentEventToOrderEventPayload(PaymentEvent paymentEvent){
        return OrderEventPayload.builder()
                .paymentId(paymentEvent.getPayment().getId().toString())
                .customerId(paymentEvent.getPayment().getCustomerId().toString())
                .orderId(paymentEvent.getPayment().getOrderId().toString())
                .price(paymentEvent.getPayment().getPrice().getAmount())
                .createdAt(paymentEvent.getCreatedAt())
                .paymentStatus(paymentEvent.getPayment().getStatus().name())
                .failureMessages(paymentEvent.getFailureMessages())
                .build();
    }
}
