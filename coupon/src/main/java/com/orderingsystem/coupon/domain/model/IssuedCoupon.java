package com.orderingsystem.coupon.domain.model;

import com.orderingsystem.common.domain.BaseEntity;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "issued_coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Getter
@Builder
public class IssuedCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID couponId;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private IssuedCouponStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;

    public static IssuedCoupon create(UUID userId, UUID couponId, LocalDateTime issuedAt, LocalDateTime expiredAt) {
        return IssuedCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(issuedAt)
                .expiredAt(expiredAt)
                .build();
    }

    public IssuedCouponStatus getDisplayStatus() {
        if (this.status == IssuedCouponStatus.ISSUED
                && this.expiredAt != null
                && this.expiredAt.isBefore(LocalDateTime.now())) {
            return IssuedCouponStatus.EXPIRED;
        }
        return this.status;
    }

}
