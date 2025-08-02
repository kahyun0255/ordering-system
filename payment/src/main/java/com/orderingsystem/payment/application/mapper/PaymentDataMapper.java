package com.orderingsystem.payment.application.mapper;

import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentDataMapper {

    public OrderEventPayload paymentEventToOrderEventPayload(PaymentEvent paymentEvent, UUID sagaId){
        return OrderEventPayload.builder()
                .paymentId(paymentEvent.getPayment().getId().toString())
                .customerId(paymentEvent.getPayment().getCustomerId().toString())
                .sagaId(sagaId.toString())
                .orderId(paymentEvent.getPayment().getOrderId().toString())
                .price(paymentEvent.getPayment().getPrice().getAmount())
                .createdAt(paymentEvent.getCreatedAt())
                .paymentStatus(paymentEvent.getPayment().getStatus().name())
                .failureMessages(paymentEvent.getFailureMessages())
                .build();
    }
}
