package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.orderingsystem.coupon.application.dto.request.CouponIssueApplicationRequest;
import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import com.orderingsystem.coupon.application.port.out.CouponIssueMessagePublisher;
import com.orderingsystem.coupon.domain.event.CouponIssuedEvent;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueCouponServiceTest {

    @Mock
    private CouponCachePort couponCachePort;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private CouponIssueMessagePublisher couponIssueMessagePublisher;

    @InjectMocks
    private IssueCouponService issueCouponService;

    @DisplayName("재고가 있고 최초 발급이면 이벤트를 발행한다.")
    @Test
    void shouldPublishEvent_whenFirstIssuanceAndStockAvailable() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(true);
        given(couponCachePort.addIssuedUser(couponId, userId)).willReturn(true);
        given(couponCachePort.decreaseStock(couponId)).willReturn(100L);

        //when
        issueCouponService.requestCouponIssuance(couponId, userId, now);

        //then
        ArgumentCaptor<CouponIssuedEvent> captor = ArgumentCaptor.forClass(CouponIssuedEvent.class);
        verify(couponIssueMessagePublisher).publish(captor.capture());

        CouponIssuedEvent event = captor.getValue();
        assertThat(event.getCouponId()).isEqualTo(couponId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getCreatedAt()).isEqualTo(now);

        verify(couponCachePort, never()).increaseStock(any());
        verify(couponCachePort, never()).removeIssuedUser(any(), any());
    }

    @DisplayName("쿠폰이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCouponDoesNotExist() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> issueCouponService.requestCouponIssuance(couponId, userId, now))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("존재하지 않는 쿠폰입니다.");

        verify(couponCachePort, never()).addIssuedUser(any(), any());
        verify(couponCachePort, never()).decreaseStock(any());
        verifyNoInteractions(couponIssueMessagePublisher);
    }

    @DisplayName("이미 해당 쿠폰을 발급한 사용자가 쿠폰 발급을 요청하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCouponAlreadyIssued() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(true);
        given(couponCachePort.addIssuedUser(couponId, userId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> issueCouponService.requestCouponIssuance(couponId, userId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 쿠폰이 발급되었습니다.");

        verify(couponCachePort, never()).decreaseStock(any());
        verifyNoInteractions(couponIssueMessagePublisher);
    }

    @DisplayName("재고 차감 결과가 음수라면 예외가 발생하고 롤백을 진행한다.")
    @Test
    void shouldRollback_whenStockDeductionResultIsNegative() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(true);
        given(couponCachePort.addIssuedUser(couponId, userId)).willReturn(true);
        given(couponCachePort.decreaseStock(couponId)).willReturn(-1L);

        //when, then
        assertThatThrownBy(() -> issueCouponService.requestCouponIssuance(couponId, userId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("쿠폰이 마감되었습니다.");

        verify(couponCachePort).increaseStock(couponId);
        verify(couponCachePort).removeIssuedUser(couponId, userId);
        verifyNoInteractions(couponIssueMessagePublisher);
    }

    @DisplayName("재고 차감 결과가 null이면 예외가 발생하고 롤백을 진행한다.")
    @Test
    void shouldRollback_whenStockDeductionResultIsNull() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(true);
        given(couponCachePort.addIssuedUser(couponId, userId)).willReturn(true);
        given(couponCachePort.decreaseStock(couponId)).willReturn(null);

        //when, then
        assertThatThrownBy(() -> issueCouponService.requestCouponIssuance(couponId, userId, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("쿠폰이 마감되었습니다.");

        verify(couponCachePort).increaseStock(couponId);
        verify(couponCachePort).removeIssuedUser(couponId, userId);
        verifyNoInteractions(couponIssueMessagePublisher);
    }

    @DisplayName("publish 중 예외가 발생하면 예외가 발생하고 롤백을 진행한다.")
    @Test
    void shouldRollback_whenPublishingFailsDuringCouponIssue() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        given(couponCachePort.exists(couponId)).willReturn(true);
        given(couponCachePort.addIssuedUser(couponId, userId)).willReturn(true);
        given(couponCachePort.decreaseStock(couponId)).willReturn(1000L);

        doThrow(new RuntimeException("publish failed"))
                .when(couponIssueMessagePublisher).publish(any(CouponIssuedEvent.class));

        //when
        assertThatThrownBy(() -> issueCouponService.requestCouponIssuance(couponId, userId, now))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠폰 발급 중 오류 발생");

        //then
        verify(couponCachePort).increaseStock(couponId);
        verify(couponCachePort).removeIssuedUser(couponId, userId);
    }

    @DisplayName("쿠폰 저장 중 요청이 비어있으면 아무 것도 하지 않는다.")
    @Test
    void shouldDoNothing_whenIssuedCouponRequestIsEmpty() {
        //when
        issueCouponService.saveIssuedCoupon(Collections.emptyList());

        //then
        verifyNoInteractions(issuedCouponRepository, couponRepository);
    }

    @DisplayName("쿠폰 저장 요청이 들어오면 쿠폰 발급 정보를 저장하고 쿠폰별 집계 수만큼 쿠폰 발급 개수를 증가시킨다.")
    @Test
    void shouldPersistIssuedCouponsAndIncrementCountByAggregate() {
        //given
        UUID coupon1 = UUID.randomUUID();
        UUID coupon2 = UUID.randomUUID();

        CouponIssueApplicationRequest r1 = mock(CouponIssueApplicationRequest.class);
        CouponIssueApplicationRequest r2 = mock(CouponIssueApplicationRequest.class);
        CouponIssueApplicationRequest r3 = mock(CouponIssueApplicationRequest.class);

        given(r1.getCouponId()).willReturn(coupon1);
        given(r2.getCouponId()).willReturn(coupon1);
        given(r3.getCouponId()).willReturn(coupon2);

        given(r1.getUserId()).willReturn(UUID.randomUUID());
        given(r2.getUserId()).willReturn(UUID.randomUUID());
        given(r3.getUserId()).willReturn(UUID.randomUUID());

        given(r1.getIssuedAt()).willReturn(LocalDateTime.now());
        given(r2.getIssuedAt()).willReturn(LocalDateTime.now());
        given(r3.getIssuedAt()).willReturn(LocalDateTime.now());

        given(r1.getExpiredAt()).willReturn(LocalDateTime.now().plusDays(30));
        given(r2.getExpiredAt()).willReturn(LocalDateTime.now().plusDays(30));
        given(r3.getExpiredAt()).willReturn(LocalDateTime.now().plusDays(30));

        List<CouponIssueApplicationRequest> requests = Arrays.asList(r1, r2, r3);

        ArgumentCaptor<List<IssuedCoupon>> saveCap = ArgumentCaptor.forClass(List.class);

        //when
        issueCouponService.saveIssuedCoupon(requests);

        //then
        verify(issuedCouponRepository).saveAll(saveCap.capture());
        List<IssuedCoupon> saved = saveCap.getValue();
        assertThat(saved).hasSize(3);

        verify(couponRepository).increaseIssuedCount(coupon1, 2L);
        verify(couponRepository).increaseIssuedCount(coupon2, 1L);
        verifyNoMoreInteractions(couponRepository);
    }

}
