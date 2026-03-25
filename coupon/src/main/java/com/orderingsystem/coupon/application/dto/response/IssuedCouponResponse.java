package com.orderingsystem.coupon.application.dto.response;

import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedCouponResponse {

    private final UUID couponId;
    private final String couponName;
    private Long issuedCouponId;
    private final IssuedCouponStatus issuedCouponStatus;
    private final LocalDateTime issuedAt;
    private final LocalDateTime usedAt;
    private final LocalDateTime expiredAt;
    private final DiscountType discountType;
    private final BigDecimal amountOff;
    private final Long percentOff;
    private final BigDecimal maxDiscountAmount;
    private final BigDecimal minOrderAmount;

}
