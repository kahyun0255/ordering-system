package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.CouponActions;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.coupon.application.dto.request.CouponRequest;
import com.orderingsystem.coupon.application.mapper.CouponDataMapper;
import com.orderingsystem.coupon.domain.exception.CouponApplicationException;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import com.orderingsystem.coupon.domain.repository.outbox.OrderOutboxRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class CouponRedemptionServiceIntegrationTest {

    @Autowired
    private CouponRedemptionService couponRedemptionService;

    @MockitoSpyBean
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @MockitoSpyBean
    private CouponDataMapper couponDataMapper;

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
        orderOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("본인이 발급받은 쿠폰이고, ISSUED 상태의 쿠폰이면 쿠폰 사용에 성공한다.")
    @Test
    void shouldSucceedUsingCoupon_whenCouponIsOwnedByUserAndStatusIsIssued() {
        //given
        UUID userId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(after.get().getUsedAt()).isNotNull();
        assertThat(after.get().getOrderId()).isEqualTo(couponRequest.getOrderId());

        assertThat(orderOutboxRepository.count()).isEqualTo(1L);
    }

    @DisplayName("쿠폰이 한 개일 때, 본인이 발급받은 쿠폰이 아닐경우 쿠폰 사용에 실패한다.")
    @Test
    void shouldFailToUseCoupon_whenCouponIsNotOwnedByUser() {
        //given
        UUID userId = UUID.randomUUID();
        UUID nonUserId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(nonUserId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(after.get().getUsedAt()).isNull();
        assertThat(after.get().getOrderId()).isNull();

        assertThat(orderOutboxRepository.count()).isEqualTo(1L);

        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), any());
    }

    @DisplayName("쿠폰이 여러 개일 때, 하나라도 본인이 발급받은 쿠폰이 아닐경우 쿠폰 사용에 실패한다.")
    @Test
    void shouldFailToUseCoupon_whenMultipleCouponsRequestedAndContainsNonOwnedCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID nonUserId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(nonUserId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);
        IssuedCoupon savedIssuedCoupon2 = issuedCouponRepository.save(issuedCoupon2);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId(), savedIssuedCoupon2.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(after.get().getUsedAt()).isNull();
        assertThat(after.get().getOrderId()).isNull();

        assertThat(orderOutboxRepository.count()).isEqualTo(1L);

        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), any());
    }

    @DisplayName("본인이 발급받은 쿠폰이지만 ISSUED 상태의 쿠폰이 아니라면 쿠폰 사용에 실패한다.")
    @ParameterizedTest(name = "[{index}] 쿠폰 상태 : {0}")
    @MethodSource("provideInvalidCouponStatuses")
    void shouldFailToUseCoupon_whenCouponIsOwnedByUserButStatusIsNotIssued(String status,
                                                                           IssuedCouponStatus issuedCouponStatus)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(issuedCouponStatus)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(issuedCouponStatus);
        assertThat(after.get().getUsedAt()).isNull();
        assertThat(after.get().getOrderId()).isNull();

        assertThat(orderOutboxRepository.count()).isEqualTo(1L);

        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), any());
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
        UUID userId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId(), 1000L))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.USE)
                .build();

        //when
        couponRedemptionService.redeem(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(after.get().getUsedAt()).isNull();
        assertThat(after.get().getOrderId()).isNull();

        assertThat(orderOutboxRepository.count()).isEqualTo(1L);

        verify(couponDataMapper).redeemCouponToCouponOrderEventPayload(any(), eq(0), any());
    }

    @DisplayName("쿠폰이 존재하고, 취소한 주문에서 사용된 쿠폰이라면 쿠폰 사용을 취소할 수 있다.")
    @Test
    void shouldSucceedCancelingCoupon_whenCouponExistsAndWasUsedInCanceledOrder() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when
        couponRedemptionService.cancelRedemption(couponRequest);

        //then
        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());

        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.ISSUED);
        assertThat(after.get().getUsedAt()).isNull();
        assertThat(after.get().getOrderId()).isNull();
    }

    @DisplayName("쿠폰이 한 개일 때, 존재하지 않는 쿠폰을 사용 취소한다면 쿠폰 사용을 취소할 수 없다.")
    @Test
    void shouldFailToCancelCoupon_whenSingleRequestedCouponDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(100000L))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("취소를 요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    @DisplayName("쿠폰이 여러 개일 때, 존재하지 않는 쿠폰을 포함해 사용 취소한다면 쿠폰 사용을 취소할 수 없다.")
    @Test
    void shouldFailToCancelCoupon_whenMultipleRequestedCouponsIncludeNonExistentCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(100000L, savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("취소를 요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    @DisplayName("쿠폰이 한 개일 때, 쿠폰이 존재하지만 USED 상태가 아닌 쿠폰을 취소 요청하면 쿠폰 사용을 취소할 수 없다.")
    @ParameterizedTest(name = "[{index}] 쿠폰 상태 : {0}")
    @MethodSource("provideNonUsedStatuses")
    void shouldFailToCancelCoupon_whenSingleCouponExistsButStatusIsNotUsed(String status,
                                                                           IssuedCouponStatus issuedCouponStatus) {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(issuedCouponStatus)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다.");

        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());
        assertThat(after.get().getStatus()).isEqualTo(issuedCouponStatus);

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    @DisplayName("쿠폰이 여러 개일 때, 하나라도 USED 상태가 아닌 쿠폰을 취소 요청하면 쿠폰 사용을 취소할 수 없다.")
    @ParameterizedTest(name = "[{index}] 쿠폰 상태 : {0}")
    @MethodSource("provideNonUsedStatuses")
    void shouldFailToCancelCoupon_whenMultipleCouponsRequestedAndContainsNonUsedStatus(String status,
                                                                                       IssuedCouponStatus issuedCouponStatus) {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(issuedCouponStatus)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);
        IssuedCoupon savedIssuedCoupon2 = issuedCouponRepository.save(issuedCoupon2);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId(), savedIssuedCoupon2.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다.");

        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());
        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);

        Optional<IssuedCoupon> after2 = issuedCouponRepository.findById(savedIssuedCoupon2.getId());
        assertThat(after2.get().getStatus()).isEqualTo(issuedCouponStatus);

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    private static Stream<Arguments> provideNonUsedStatuses() {
        return Stream.of(
                Arguments.of("사용하지 않은 쿠폰", IssuedCouponStatus.ISSUED),
                Arguments.of("만료된 쿠폰", IssuedCouponStatus.EXPIRED),
                Arguments.of("회수된 쿠폰", IssuedCouponStatus.REVOKED)
        );
    }

    @DisplayName("쿠폰이 한 개일 때, 취소한 주문에서 사용되지 않은 쿠폰을 취소요청하면 쿠폰 취소에 실패한다.")
    @Test
    void shouldFailToCancelCoupon_whenSingleRequestedCouponWasNotUsedInTheCanceledOrder() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID invalidOrderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(invalidOrderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다.");

        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());
        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(after.get().getUsedAt()).isNotNull();
        assertThat(after.get().getOrderId()).isNotNull();

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    @DisplayName("쿠폰이 여러 개일 때, 하나라도 취소한 주문에서 사용되지 않은 쿠폰을 취소요청하면 쿠폰 취소에 실패한다.")
    @Test
    void shouldFailToCancelCoupon_whenMultipleCouponsRequestedAndContainsMismatchedOrderCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID invalidOrderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(invalidOrderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);
        IssuedCoupon savedIssuedCoupon2 = issuedCouponRepository.save(issuedCoupon2);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId(), savedIssuedCoupon2.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("해당 주문 정보와 일치하지 않는 쿠폰이 포함되어 있습니다.");

        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());
        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(after.get().getUsedAt()).isNotNull();
        assertThat(after.get().getOrderId()).isNotNull();

        Optional<IssuedCoupon> after2 = issuedCouponRepository.findById(savedIssuedCoupon2.getId());
        assertThat(after2.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(after2.get().getUsedAt()).isNotNull();
        assertThat(after.get().getOrderId()).isNotNull();

        verify(issuedCouponRepository, never()).redeemCancelCoupons(any(), any(), any());
    }

    @DisplayName("취소 상태로 업데이트한 쿠폰 수량과 요청한 쿠폰 수량이 일치하지 않으면 쿠폰 사용 취소에 실패한다.")
    @Test
    void shouldFailToCancelCoupon_whenUpdatedCountDoesNotMatchRequestedCount() {
        //given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.USED)
                .orderId(orderId)
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .expiredAt(LocalDateTime.now().plusDays(1))
                .issuedAt(LocalDateTime.now().minusDays(3))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        CouponRequest couponRequest = CouponRequest.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .sagaId(UUID.randomUUID())
                .issuedCouponIds(List.of(savedIssuedCoupon.getId()))
                .createdAt(Instant.now())
                .failureMessages(null)
                .action(CouponActions.ROLLBACK)
                .build();

        doReturn(0).when(issuedCouponRepository).redeemCancelCoupons(any(), any(), any());

        //when, then
        assertThatThrownBy(() -> couponRedemptionService.cancelRedemption(couponRequest))
                .isInstanceOf(CouponApplicationException.class)
                .hasMessage("쿠폰 취소 실패.");

        Optional<IssuedCoupon> after = issuedCouponRepository.findById(savedIssuedCoupon.getId());
        assertThat(after.get().getStatus()).isEqualTo(IssuedCouponStatus.USED);
        assertThat(after.get().getUsedAt()).isNotNull();
        assertThat(after.get().getOrderId()).isNotNull();
    }

}
