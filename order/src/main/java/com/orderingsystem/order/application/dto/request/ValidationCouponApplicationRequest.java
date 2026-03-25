package com.orderingsystem.order.application.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidationCouponApplicationRequest {

    private UUID customerId;
    private List<Long> couponIds;
    private BigDecimal totalOrderAmount;

}
