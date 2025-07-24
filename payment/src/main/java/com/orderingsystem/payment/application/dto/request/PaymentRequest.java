package com.orderingsystem.payment.application.dto.request;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.payment.domain.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class PaymentRequest {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private Instant createdAt;
    private PaymentOrderStatus paymentOrderStatus;

    public Payment toPayment(){
        return Payment.builder()
                .orderId(this.orderId)
                .customerId(this.customerId)
                .price(new Money(this.price))
                .build();
    }
}
