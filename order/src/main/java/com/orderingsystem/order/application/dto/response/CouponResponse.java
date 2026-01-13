package com.orderingsystem.order.application.dto.response;

import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CouponResponse {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private UUID sagaId;
    private Instant createdAt;
    private List<Long> issuedCouponId;
    private int updatedCount;
    private List<String> failureMessages;
    private IssuedCouponStatus issuedCouponStatus;

}
