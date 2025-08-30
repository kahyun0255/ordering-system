package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.order.application.dto.response.PaymentResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentResponseMessage {

    private UUID sagaId;
    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private OffsetDateTime createdAt;
    private String paymentStatus;
    private List<String> failureMessages;

    public PaymentResponse toPaymentResponse(UUID id) {
        return PaymentResponse.builder()
                .id(id)
                .sagaId(this.sagaId)
                .paymentId(this.paymentId)
                .orderId(this.orderId)
                .customerId(this.customerId)
                .price(this.price)
                .createdAt(this.createdAt.toInstant())
                .paymentStatus(this.paymentStatus)
                .failureMessages(this.failureMessages)
                .build();
    }
}
