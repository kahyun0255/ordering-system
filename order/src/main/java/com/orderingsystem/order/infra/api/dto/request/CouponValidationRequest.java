package com.orderingsystem.order.infra.api.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponValidationRequest {

    private UUID customerId;
    private List<Long> couponIds;
    private BigDecimal totalOrderAmount;
    private UUID sagaId;

}
