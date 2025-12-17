package com.orderingsystem.coupon.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

}
