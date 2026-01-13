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

    private UUID orderId;
    private UUID customerId;
    private UUID sagaId;
    private OffsetDateTime createdAt;
    private List<Long> issuedCouponId;
    private int updatedCount;
    private List<String> failureMessages;
    private String issuedCouponStatus;

    public CouponResponse toCouponResponse(UUID id) {
        return CouponResponse.builder()
                .id(id)
                .orderId(this.orderId)
                .customerId(this.customerId)
                .sagaId(this.sagaId)
                .createdAt(this.createdAt.toInstant())
                .issuedCouponId(this.issuedCouponId)
                .updatedCount(this.updatedCount)
                .failureMessages(this.failureMessages)
                .issuedCouponStatus(null) //TODO : 롤백 처리 변경 후 제거
                .build();
    }
}
