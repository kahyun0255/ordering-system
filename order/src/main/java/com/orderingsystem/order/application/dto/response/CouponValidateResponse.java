package com.orderingsystem.order.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CouponValidateResponse {

    private boolean valid;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;

}
