package com.orderingsystem.coupon.infra.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class CouponStartRunnerTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponDistributionScheduler couponDistributionScheduler;

    @InjectMocks
    private CouponStartRunner couponStartRunner;

    @DisplayName("서버 시작 시 예약된 쿠폰이 있다면 스케줄러에 등록한다.")
    @Test
    void shouldRegisterScheduledCouponsToScheduler_onServerStartup() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon1 = Coupon.builder()
                .couponId(UUID.randomUUID())
                .issueLimit(100L)
                .validFrom(now.plusHours(1))
                .validUntil(now.plusDays(1))
                .status(CouponStatus.SCHEDULED)
                .discountType(DiscountType.FIXED_AMOUNT)
                .build();

        Coupon coupon2 = Coupon.builder()
                .couponId(UUID.randomUUID())
                .issueLimit(50L)
                .validFrom(now.plusHours(2))
                .validUntil(now.plusDays(2))
                .status(CouponStatus.SCHEDULED)
                .discountType(DiscountType.PERCENTAGE)
                .build();

        given(couponRepository.findAllByStatusAndValidUntilAfter(eq(CouponStatus.SCHEDULED),
                any(LocalDateTime.class))).willReturn(List.of(coupon1, coupon2));

        //when
        couponStartRunner.run(mock(ApplicationArguments.class));

        //then
        verify(couponDistributionScheduler, times(1)).scheduleCouponStart(coupon1.getCouponId(),
                coupon1.getIssueLimit(), coupon1.getValidFrom(), coupon1.getValidUntil());
        verify(couponDistributionScheduler, times(1)).scheduleCouponStart(coupon2.getCouponId(),
                coupon2.getIssueLimit(), coupon2.getValidFrom(), coupon2.getValidUntil());
    }

    @DisplayName("서버 시작 시 예약된 쿠폰이 없다면 스케줄러를 호출하지 않는다.")
    @Test
    void shouldNotInvokeScheduler_whenNoScheduledCouponsExistOnStartup() {
        //given
        given(couponRepository.findAllByStatusAndValidUntilAfter(any(), any())).willReturn(Collections.emptyList());

        //when
        couponStartRunner.run(null);

        //then
        verify(couponDistributionScheduler, never()).scheduleCouponStart(any(), any(), any(), any());
    }

}
