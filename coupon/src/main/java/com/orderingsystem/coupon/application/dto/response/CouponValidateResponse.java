package com.orderingsystem.coupon.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponValidateResponse {

    private boolean valid;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;

}
