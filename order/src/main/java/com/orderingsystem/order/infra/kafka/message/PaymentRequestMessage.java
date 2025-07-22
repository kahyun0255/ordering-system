package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.common.domain.status.PaymentOrderStatus;
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
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private Instant createdAt;
    private PaymentOrderStatus paymentOrderStatus;

}
