package com.orderingsystem.payment.infra.kafka.message;

import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PaymentRequestMessage {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private Instant createdAt;
    private PaymentOrderStatus paymentOrderStatus;

    public PaymentRequest toPaymentRequest() {
        return PaymentRequest.builder()
                .id(this.getId())
                .sagaId(this.getSagaId())
                .orderId(this.getOrderId())
                .customerId(this.getCustomerId())
                .price(this.getPrice())
                .createdAt(this.getCreatedAt())
                .paymentOrderStatus(this.getPaymentOrderStatus())
                .build();
    }
}
