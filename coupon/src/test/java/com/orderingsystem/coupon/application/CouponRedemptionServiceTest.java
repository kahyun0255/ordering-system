package com.orderingsystem.coupon.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.CouponActions;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.coupon.application.dto.request.CouponRequest;
import com.orderingsystem.coupon.application.mapper.CouponDataMapper;
import com.orderingsystem.coupon.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponRedemptionService 단위 테스트")
class CouponRedemptionServiceTest {

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @Mock
    private OrderOutboxHelper orderOutboxHelper;

    @Mock
    private CouponDataMapper couponDataMapper;

    @InjectMocks
    private CouponRedemptionService couponRedemptionService;

    @DisplayName("본인이 발급받은 쿠폰이고, ISSUED 상태의 쿠폰이면 쿠폰 사용에 성공한다.")
    @Test
    void shouldSucceedUsingCoupon_whenCouponIsOwnedByUserAndStatusIsIssued() {
        //given
        Long couponId = 10L;
        UUID userId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(List.of(issuedCoupon));
        given(issuedCoupon.getUserId()).willReturn(userId);
        given(issuedCoupon.getDisplayStatus()).willReturn(IssuedCouponStatus.ISSUED);
        given(issuedCouponRepository.redeemCoupons(any(), any(), any(), any(), any())).willReturn(1);

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, times(1)).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId)), any());
        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
    }

    @DisplayName("쿠폰이 한 개일 때, 본인이 발급받은 쿠폰이 아닐경우 쿠폰 사용에 실패한다.")
    @Test
    void shouldFailToUseCoupon_whenCouponIsNotOwnedByUser() {
        //given
        Long couponId = 10L;
        UUID userId = UUID.randomUUID();
        UUID nonUserId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(List.of(issuedCoupon));
        given(issuedCoupon.getUserId()).willReturn(nonUserId);

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, never()).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId)), any());
        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), anyList());
    }

    @DisplayName("쿠폰이 여러 개일 때, 하나라도 본인이 발급받은 쿠폰이 아닐경우 쿠폰 사용에 실패한다.")
    @Test
    void shouldFailToUseCoupon_whenMultipleCouponsRequestedAndContainsNonOwnedCoupon() {
        //given
        Long couponId = 10L;
        Long couponId2 = 20L;
        UUID userId = UUID.randomUUID();
        UUID nonUserId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId, couponId2))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon1 = mock(IssuedCoupon.class);
        IssuedCoupon issuedCoupon2 = mock(IssuedCoupon.class);
        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(
                List.of(issuedCoupon1, issuedCoupon2));

        given(issuedCoupon1.getUserId()).willReturn(userId);
        given(issuedCoupon2.getUserId()).willReturn(nonUserId);

        given(issuedCoupon1.getDisplayStatus()).willReturn(IssuedCouponStatus.ISSUED);

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, never()).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId2)), any());
        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), anyList());
    }

    @DisplayName("본인이 발급받은 쿠폰이지만 ISSUED 상태의 쿠폰이 아니라면 쿠폰 사용에 실패한다.")
    @ParameterizedTest(name = "[{index}] 쿠폰 상태 : {0}")
    @MethodSource("provideInvalidCouponStatuses")
    void shouldFailToUseCoupon_whenCouponIsOwnedByUserButStatusIsNotIssued(String status,
                                                                           IssuedCouponStatus issuedCouponStatus)
            throws Exception {
        //given
        Long couponId = 10L;
        UUID userId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(List.of(issuedCoupon));
        given(issuedCoupon.getUserId()).willReturn(userId);
        given(issuedCoupon.getDisplayStatus()).willReturn(issuedCouponStatus);

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, never()).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId)), any());
        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), anyList());
    }

    private static Stream<Arguments> provideInvalidCouponStatuses() {
        return Stream.of(
                Arguments.of("만료된 쿠폰", IssuedCouponStatus.EXPIRED),
                Arguments.of("회수된 쿠폰", IssuedCouponStatus.REVOKED),
                Arguments.of("사용된 쿠폰", IssuedCouponStatus.USED)
        );
    }

    @DisplayName("존재하지 않는 쿠폰을 포함해 쿠폰 사용을 요청한 경우, 쿠폰 사용에 실패한다.")
    @Test
    void shouldFailToUseCoupon_whenRequestContainsNonExistentCoupon() {
        //given
        Long couponId = 10L;
        UUID userId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId, 100L))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);
        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(List.of(issuedCoupon));

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, never()).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId)), any());
        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), anyList());
    }

    @DisplayName("검증은 통과했으나 동시성 이슈 등으로 DB 업데이트 개수가 요청 개수와 다를 경우 실패 처리한다.")
    @Test
    void shouldFailToUseCoupon_whenUpdateCountDoesNotMatchRequestCount() {
        //given
        Long couponId = 10L;
        UUID userId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(couponId))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        given(issuedCouponRepository.findAllById(couponRequest.getIssuedCouponIds())).willReturn(List.of(issuedCoupon));
        given(issuedCoupon.getUserId()).willReturn(userId);
        given(issuedCoupon.getDisplayStatus()).willReturn(IssuedCouponStatus.ISSUED);

        given(issuedCouponRepository.redeemCoupons(any(), any(), any(), any(), any())).willReturn(0);

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllById(anyList());
        verify(issuedCouponRepository, times(1)).redeemCoupons(any(), eq(couponRequest.getOrderId()), any(),
                eq(List.of(couponId)), any());

        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(
                any(),
                eq(0),
                argThat(list -> list.contains("쿠폰 사용 중 오류 발생."))
        );

        verify(orderOutboxHelper, times(1)).saveOrderOutboxMessage(any(), any(), any());
    }

    @DisplayName("쿠폰이 한 개일 경우, 주문 ID로 사용한 쿠폰을 조회해 쿠폰을 취소할 수 있다.")
    @Test
    void shouldSucceedCancelingSingleCoupon_whenFindingUsedCouponByOrderId() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        given(issuedCouponRepository.findAllByOrderId(orderId)).willReturn(List.of(issuedCoupon));
        given(issuedCoupon.getOrderId()).willReturn(orderId);
        given(issuedCoupon.getDisplayStatus()).willReturn(IssuedCouponStatus.USED);
        given(issuedCouponRepository.redeemCancelCoupons(IssuedCouponStatus.ISSUED, orderId,
                IssuedCouponStatus.USED)).willReturn(1);

        //when
        couponRedemptionService.cancelRedemption(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllByOrderId(orderId);
        verify(issuedCouponRepository, times(1)).redeemCancelCoupons(eq(IssuedCouponStatus.ISSUED),
                eq(orderId), eq(IssuedCouponStatus.USED));
    }

    @DisplayName("쿠폰이 여러 개일 경우, 주문 ID로 사용한 쿠폰을 조회해 쿠폰을 취소할 수 있다.")
    @Test
    void shouldSucceedCancelingMultipleCoupons_whenFindingUsedCouponsByOrderId() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        IssuedCoupon issuedCoupon1 = mock(IssuedCoupon.class);
        IssuedCoupon issuedCoupon2 = mock(IssuedCoupon.class);

        given(issuedCouponRepository.findAllByOrderId(orderId)).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(issuedCoupon1.getOrderId()).willReturn(orderId);
        given(issuedCoupon1.getDisplayStatus()).willReturn(IssuedCouponStatus.USED);
        given(issuedCoupon2.getOrderId()).willReturn(orderId);
        given(issuedCoupon2.getDisplayStatus()).willReturn(IssuedCouponStatus.USED);
        given(issuedCouponRepository.redeemCancelCoupons(IssuedCouponStatus.ISSUED, orderId,
                IssuedCouponStatus.USED)).willReturn(2);

        //when
        couponRedemptionService.cancelRedemption(couponRequest);

        //then
        verify(issuedCouponRepository, times(1)).findAllByOrderId(orderId);
        verify(issuedCouponRepository, times(1)).redeemCancelCoupons(eq(IssuedCouponStatus.ISSUED),
                eq(orderId), eq(IssuedCouponStatus.USED));
    }

}
