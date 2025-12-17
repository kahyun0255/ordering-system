package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponManagementServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponCachePort couponCachePort;

    @InjectMocks
    private CouponManagementService couponManagementService;

    @DisplayName("쿠폰이 존재하면 쿠폰을 정지할 수 있다.")
    @Test
    void shouldDeactivateCoupon_whenCouponExists() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        //when
        couponManagementService.pause(couponId, userId);

        //then
        verify(coupon, times(1)).pause();
        verify(couponCachePort, times(1)).deleteCouponStock(couponId);
    }

    @DisplayName("쿠폰이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCouponDoesNotExist() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> couponManagementService.pause(couponId, userId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰이 존재하지 않습니다.");

        verify(couponCachePort, never()).deleteCouponStock(couponId);
    }

    @DisplayName("쿠폰이 존재하면 쿠폰을 재시작할 수 있다.")
    @Test
    void shouldReactivateCoupon_whenCouponExists() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(coupon.getIssueLimit()).willReturn(100L);
        given(coupon.getIssuedCount()).willReturn(10L);
        given(coupon.getValidUntil()).willReturn(LocalDateTime.now());

        //when
        couponManagementService.resume(couponId, userId);

        //then
        verify(coupon, times(1)).resume();
        verify(couponCachePort, times(1)).enableCoupon(eq(couponId), anyLong(), any(LocalDateTime.class));
    }

    @DisplayName("쿠폰이 존재하지 않으면 쿠폰 재시작 시 예외가 발생한다.")
    @Test
    void shouldThrowException_whenTryingToReactivateNonExistentCoupon() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> couponManagementService.resume(couponId, userId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰이 존재하지 않습니다.");

        verify(couponCachePort, never()).enableCoupon(eq(couponId), anyLong(), any(LocalDateTime.class));
    }

    @DisplayName("쿠폰이 존재하면 쿠폰을 종료할 수 있다.")
    @Test
    void shouldExpireCoupon_whenCouponExists() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Coupon coupon = mock(Coupon.class);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        //when
        couponManagementService.terminate(couponId, userId);

        //then
        verify(coupon, times(1)).terminate();
        verify(couponCachePort, times(1)).setExpireIssuedUserKey(couponId);
    }

    @DisplayName("쿠폰이 존재하지 않으면 쿠폰 종료시 예외가 발생한다.")
    @Test
    void shouldThrowException_whenExpiringNonExistentCoupon() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> couponManagementService.terminate(couponId, userId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰이 존재하지 않습니다.");

        verify(couponCachePort, never()).deleteCouponStock(couponId);
        verify(couponCachePort, never()).setExpireIssuedUserKey(couponId);
    }

}
