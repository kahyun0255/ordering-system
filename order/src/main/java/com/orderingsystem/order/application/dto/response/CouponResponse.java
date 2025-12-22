package com.orderingsystem.order.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CouponResponse {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID customerId;
    private Instant createdAt;
    private String issuedCouponStatus;
    private List<String> failureMessages;

}
