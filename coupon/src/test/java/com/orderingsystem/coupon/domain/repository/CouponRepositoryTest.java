package com.orderingsystem.coupon.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private EntityManager em;

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
    }

    @DisplayName("쿠폰의 발급 수량을 지정된 숫자만큼 증가시킨다.")
    @Test
    void shouldIncrementCouponIssueCount_byGivenAmount() {
        //given
        Coupon coupon = createCoupon();

        couponRepository.save(coupon);

        Long increaseAmount = 5L;

        //when
        couponRepository.increaseIssuedCount(coupon.getCouponId(), increaseAmount);
        em.clear();

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getIssuedCount()).isEqualTo(increaseAmount);
    }

    @DisplayName("쿠폰의 상태를 변경한다.")
    @Test
    void shouldUpdateCouponStatus() {
        //given
        Coupon coupon = createCoupon();

        couponRepository.save(coupon);

        //when
        couponRepository.updateStatus(coupon.getCouponId(), CouponStatus.PAUSED);
        em.clear();

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.PAUSED);
    }

    @DisplayName("만료일이 지난 활성 쿠폰들을 일괄적으로 EXPIRED 상태로 변경한다.")
    @Test
    void shouldExpireActiveCouponsPastValidUntil() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon expiredCoupon = createCoupon(now.minusDays(1), CouponStatus.ACTIVE);
        Coupon validCoupon = createCoupon(now.plusDays(1), CouponStatus.ACTIVE);
        Coupon alreadyExpired = createCoupon(now.minusDays(1), CouponStatus.EXPIRED);

        couponRepository.saveAll(List.of(expiredCoupon, validCoupon, alreadyExpired));

        //when
        int updateCount = couponRepository.bulkExpireCoupons(now);

        //then
        assertThat(updateCount).isEqualTo(1);

        Coupon afterExpiredCoupon = couponRepository.findById(expiredCoupon.getCouponId()).orElseThrow();
        assertThat(afterExpiredCoupon.getStatus()).isEqualTo(CouponStatus.EXPIRED);

        Coupon afterValidCoupon = couponRepository.findById(validCoupon.getCouponId()).orElseThrow();
        assertThat(afterValidCoupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);

        Coupon afterAlreadyExpired = couponRepository.findById(alreadyExpired.getCouponId()).orElseThrow();
        assertThat(afterAlreadyExpired.getStatus()).isEqualTo(CouponStatus.EXPIRED);
    }

    private Coupon createCoupon(LocalDateTime validUntil, CouponStatus couponStatus) {
        return Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(couponStatus)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(validUntil)
                .issueLimit(1000L)
                .issuedCount(0L)
                .build();
    }

    private Coupon createCoupon() {
        return createCoupon(LocalDateTime.of(2025, 12, 20, 0, 0), CouponStatus.ACTIVE);
    }

}
