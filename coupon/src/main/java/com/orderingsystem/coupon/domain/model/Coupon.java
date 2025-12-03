package com.orderingsystem.coupon.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
@Builder
public class Coupon extends AggregateRoot {

    @Id
    private UUID couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal amountOff;

    @Min(0)
    @Max(100)
    private Long percentOff;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal maxDiscountAmount;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal minDiscountAmount;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @PositiveOrZero
    private Long issueLimit;

    @PositiveOrZero
    private Long issuedCount;

}
