package com.orderingsystem.coupon.application.dto.request;

import com.orderingsystem.coupon.domain.model.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCouponApplicationRequest {

    private DiscountType discountType;
    private BigDecimal amountOff;
    private Long percentOff;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Long issueLimit;

}
