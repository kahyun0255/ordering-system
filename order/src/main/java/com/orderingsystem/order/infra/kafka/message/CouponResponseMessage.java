package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.order.application.dto.response.CouponResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CouponResponseMessage {

    private UUID sagaId;
    private UUID orderId;
    private UUID customerId;
    private OffsetDateTime createdAt;
    private String issuedCouponStatus;
    private List<String> failureMessages;

    public CouponResponse toCouponResponse(UUID id) {
        return CouponResponse.builder()
                .id(id)
                .sagaId(this.sagaId)
                .orderId(this.orderId)
                .customerId(this.customerId)
                .issuedCouponStatus(this.issuedCouponStatus)
                .createdAt(this.createdAt.toInstant())
                .failureMessages(this.failureMessages)
                .build();
    }
}
