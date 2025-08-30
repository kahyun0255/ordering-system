package com.orderingsystem.order.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PaymentResponse {

    private UUID id;
    private UUID sagaId;
    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal price;
    private Instant createdAt;
    private String paymentStatus;
    private List<String> failureMessages;

}
