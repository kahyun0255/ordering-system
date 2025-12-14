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
import java.util.Objects;
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

    private String name;

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
    private Integer validDays;

    @PositiveOrZero
    private Long issueLimit;

    @PositiveOrZero
    private Long issuedCount;

    public static Coupon create(DiscountType discountType, BigDecimal amountOff, Long percentOff,
                                BigDecimal maxDiscountAmount, BigDecimal minDiscountAmount, LocalDateTime validFrom,
                                LocalDateTime validUntil, Long issueLimit, String name, Integer validDays) {
        return Coupon.builder()
                .couponId(UUID.randomUUID())
                .discountType(discountType)
                .status(CouponStatus.SCHEDULED)
                .amountOff(amountOff)
                .percentOff(percentOff)
                .maxDiscountAmount(maxDiscountAmount)
                .minDiscountAmount(minDiscountAmount)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .issueLimit(issueLimit)
                .issuedCount(0L)
                .name(name)
                .validDays(validDays)
                .build();
    }

    public void increaseIssuedCount() {
        this.issuedCount++;
    }

    public void pause() {
        this.status = CouponStatus.PAUSED;
    }

    public void resume() {
        if (this.status != CouponStatus.PAUSED) {
            throw new IllegalStateException("정지된 쿠폰만 재시작할 수 있습니다.");
        }
        this.status = CouponStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coupon coupon = (Coupon) o;
        return Objects.equals(couponId, coupon.couponId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(couponId);
    }
}
