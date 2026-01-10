package com.orderingsystem.coupon.application.dto.request;

import com.orderingsystem.common.domain.status.CouponActions;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponRequest {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private UUID sagaId;
    private List<Long> issuedCouponIds;
    private Instant createdAt;
    private List<String> failureMessages;
    private CouponActions action;

}
