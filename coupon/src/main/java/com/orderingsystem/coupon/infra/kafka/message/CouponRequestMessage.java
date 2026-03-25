package com.orderingsystem.coupon.infra.kafka.message;

import com.orderingsystem.common.domain.status.CouponActions;
import com.orderingsystem.coupon.application.dto.request.CouponRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponRequestMessage {

    private UUID orderId;
    private UUID customerId;
    private UUID sagaId;
    private List<String> issuedCouponId;
    private OffsetDateTime createdAt;
    private List<String> failureMessage;
    private String action;

    public CouponRequest toCouponRequest(UUID id) {
        List<Long> couponIds = (this.issuedCouponId == null)
                ? List.of()
                : this.issuedCouponId.stream().map(Long::parseLong).toList();

        return CouponRequest.builder()
                .id(id)
                .orderId(this.getOrderId())
                .userId(this.getCustomerId())
                .sagaId(this.getSagaId())
                .issuedCouponIds(couponIds)
                .createdAt(this.getCreatedAt().toInstant())
                .failureMessages(failureMessage)
                .action(CouponActions.valueOf(action))
                .build();
    }

}
