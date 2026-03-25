package com.orderingsystem.coupon.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class IssuedCouponRepositoryTest {

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
    }

    @DisplayName("만료일이 지난 발급된 쿠폰을 일괄적으로 EXPIRED 상태로 변경한다.")
    @Test
    void shouldExpireIssuedCouponsPastValidUntil() {
        //given
        LocalDateTime now = LocalDateTime.now();
        UUID couponId = UUID.randomUUID();

        IssuedCoupon target = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(couponId)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(now.minusDays(10))
                .expiredAt(now.minusDays(1))
                .build();

        IssuedCoupon alive = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(couponId)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(now.minusDays(1))
                .expiredAt(now.plusDays(1))
                .build();

        IssuedCoupon used = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(couponId)
                .status(IssuedCouponStatus.USED)
                .issuedAt(now.minusDays(10))
                .expiredAt(now.minusDays(1))
                .usedAt(now.minusDays(2))
                .build();

        issuedCouponRepository.saveAll(List.of(target, alive, used));

        //when
        int count = issuedCouponRepository.bulkExpireIssuedCoupons(now);

        //then
        assertThat(count).isEqualTo(1);

        IssuedCoupon afterTarget = issuedCouponRepository.findById(target.getId()).orElseThrow();
        assertThat(afterTarget.getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);

        IssuedCoupon afterAlive = issuedCouponRepository.findById(alive.getId()).orElseThrow();
        assertThat(afterAlive.getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);

        IssuedCoupon afterUsed = issuedCouponRepository.findById(used.getId()).orElseThrow();
        assertThat(afterUsed.getStatus()).isEqualTo(IssuedCouponStatus.USED);
    }

    @DisplayName("발급된 쿠폰 ID 목록과 일치하고 기대 상태와 일치하면 쿠폰을 사용 처리한다.")
    @Test
    void shouldRedeemCouponsWhenIdsAndStatusMatch() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        IssuedCoupon coupon1 = createIssuedCoupon(userId, couponId, IssuedCouponStatus.ISSUED, now);
        IssuedCoupon coupon2 = createIssuedCoupon(userId, couponId, IssuedCouponStatus.ISSUED, now);

        IssuedCoupon expiredCoupon = createIssuedCoupon(userId, couponId, IssuedCouponStatus.EXPIRED, now);

        issuedCouponRepository.saveAll(List.of(coupon1, coupon2, expiredCoupon));

        List<Long> targetIds = List.of(coupon1.getId(), coupon2.getId(), expiredCoupon.getId());

        //when
        int count = issuedCouponRepository.redeemCoupons(
                IssuedCouponStatus.USED,
                orderId,
                now,
                targetIds,
                IssuedCouponStatus.ISSUED
        );

        //then
        assertThat(count).isEqualTo(2);

        List<IssuedCoupon> updatedCoupons = issuedCouponRepository.findAllById(List.of(coupon1.getId(), coupon2.getId()));
        assertThat(updatedCoupons).allSatisfy(coupon -> {
            assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.USED);
            assertThat(coupon.getUsedAt()).isEqualTo(now);
            assertThat(coupon.getOrderId()).isEqualTo(orderId);
        });

        IssuedCoupon untouchedCoupon = issuedCouponRepository.findById(expiredCoupon.getId()).orElseThrow();
        assertThat(untouchedCoupon.getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
        assertThat(untouchedCoupon.getOrderId()).isNull();
    }

    private IssuedCoupon createIssuedCoupon(UUID userId, UUID couponId, IssuedCouponStatus status, LocalDateTime now) {
        return IssuedCoupon.builder()
                .userId(userId)
                .couponId(couponId)
                .status(status)
                .issuedAt(now.minusDays(1))
                .expiredAt(now.plusDays(10))
                .build();
    }

}
