package com.orderingsystem.coupon.application.dto.response;

import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponResponse {

    private final UUID couponId;
    private final String couponName;
    private final DiscountType discountType;
    private final CouponStatus couponStatus;
    private final BigDecimal amountOff;
    private final Long percentOff;
    private final BigDecimal maxDiscountAmount;
    private final BigDecimal minDiscountAmount;
    private final LocalDateTime validFrom;
    private final LocalDateTime validUntil;
    private final Integer validDays;
    private final Long issueLimit;
    private final Long issuedCount;

}
