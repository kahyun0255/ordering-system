package com.orderingsystem.coupon.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CouponExpirationSchedulerTest {

    @Autowired
    private CouponExpirationScheduler couponExpirationScheduler;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
        issuedCouponRepository.deleteAllInBatch();
    }

    @DisplayName("스케줄러가 실행되면 만료된 쿠폰의 상태가 변경된다.")
    @Test
    void shouldUpdateExpiredCouponStatus_whenSchedulerRuns() {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusMinutes(1))
                .issueLimit(1000L)
                .build();

        couponRepository.save(coupon);

        //when
        couponExpirationScheduler.expireCoupons();

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    @DisplayName("스케줄러가 실행되면 만료된 발급 쿠폰(IssuedCoupon)의 상태가 변경된다.")
    @Test
    void shouldUpdateExpiredIssuedCouponStatus_whenSchedulerRuns() {
        //given
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now().minusDays(2))
                .expiredAt(LocalDateTime.now().minusMinutes(1))
                .build();

        issuedCouponRepository.save(issuedCoupon);

        //when
        couponExpirationScheduler.expireCoupons();

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(issuedCoupon.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
    }

}
