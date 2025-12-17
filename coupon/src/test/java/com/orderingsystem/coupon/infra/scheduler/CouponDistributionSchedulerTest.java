package com.orderingsystem.coupon.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.coupon.application.IssueCouponService;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.infra.redis.RedisCouponRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class CouponDistributionSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private RedisCouponRepository redisCouponRepository;

    @Mock
    private IssueCouponService issueCouponService;

    @InjectMocks
    private CouponDistributionScheduler couponDistributionScheduler;

    @DisplayName("시작 시간이 현재보다 과거라면, 스케줄링 없이 즉시 Redis 등록 로직이 실행된다.")
    @Test
    void shouldRegisterInRedisImmediately_whenStartTimeIsInThePast() {
        //given
        UUID couponId = UUID.randomUUID();
        LocalDateTime pastDate = LocalDateTime.now().minusHours(1);

        Coupon coupon = mock(Coupon.class);

        given(coupon.getValidFrom()).willReturn(pastDate);
        given(coupon.getIssueLimit()).willReturn(100L);
        given(coupon.getValidUntil()).willReturn(LocalDateTime.now().plusDays(1));

        //when
        couponDistributionScheduler.scheduleCouponStart(
                couponId, coupon.getIssueLimit(), coupon.getValidFrom(), coupon.getValidUntil());

        //then
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        verify(redisCouponRepository).enableCoupon(any(), any(), any());
        verify(issueCouponService).updateCouponStatus(eq(couponId), eq(CouponStatus.ACTIVE));
    }

    @DisplayName("시작 시간이 현재보다 미래라면, TaskScheduler에 작업이 예약된다.")
    @Test
    void shouldScheduleTask_whenStartTimeIsInTheFuture() {
        //given
        UUID couponId = UUID.randomUUID();
        LocalDateTime futureDate = LocalDateTime.now().plusHours(1);
        Instant futureInstant = futureDate.atZone(ZoneId.systemDefault()).toInstant();

        Coupon coupon = mock(Coupon.class);

        given(coupon.getValidFrom()).willReturn(futureDate);
        given(coupon.getIssueLimit()).willReturn(100L);
        given(coupon.getValidUntil()).willReturn(LocalDateTime.now().plusDays(1));

        //when
        couponDistributionScheduler.scheduleCouponStart(couponId, coupon.getIssueLimit(), coupon.getValidFrom(),
                coupon.getValidUntil());

        //then
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(taskScheduler).schedule(runnableArgumentCaptor.capture(), instantArgumentCaptor.capture());

        assertThat(instantArgumentCaptor.getValue().getEpochSecond()).isEqualTo(futureInstant.getEpochSecond());

        Runnable schedulerTask = runnableArgumentCaptor.getValue();
        schedulerTask.run();

        verify(redisCouponRepository).enableCoupon(couponId, coupon.getIssueLimit(), coupon.getValidUntil());
        verify(issueCouponService).updateCouponStatus(couponId, CouponStatus.ACTIVE);
    }

}
