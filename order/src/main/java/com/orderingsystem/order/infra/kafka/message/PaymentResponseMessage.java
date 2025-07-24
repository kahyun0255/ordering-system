package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.order.application.dto.response.PaymentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentResponseMessage {

    private UUID id;
    private UUID sagaId;
    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private Instant createdAt;
    private String paymentStatus;
    private List<String> failureMessages;

    public PaymentResponse toPaymentResponse() {
        return PaymentResponse.builder()
                .id(this.id)
                .sagaId(this.sagaId)
                .paymentId(this.paymentId)
                .orderId(this.orderId)
                .customerId(this.customerId)
                .price(this.price)
                .createdAt(this.createdAt)
                .paymentStatus(this.paymentStatus)
                .failureMessages(this.failureMessages)
                .build();
    }
}
