package com.orderingsystem.coupon.application.dto.request;

import com.orderingsystem.coupon.domain.model.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CreateCouponApplicationRequest {

    private DiscountType discountType;
    private String name;
    private BigDecimal amountOff;
    private Long percentOff;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Long issueLimit;

}
