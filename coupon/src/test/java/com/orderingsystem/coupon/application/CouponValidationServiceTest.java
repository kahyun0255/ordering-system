package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.orderingsystem.coupon.application.dto.response.CouponValidationResponse;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import com.orderingsystem.coupon.presentation.request.CouponValidationRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponValidationServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private CouponValidationService couponValidationService;

    @DisplayName("정액제 할인 쿠폰 사용시 올바르게 계산된다.")
    @Test
    void shouldCalculateDiscountCorrectly_whenUsingFixedAmountCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualTo(discountAmount);
        assertThat(response.getFinalAmount()).isEqualTo(orderAmount.subtract(discountAmount));
        assertThat(response.getMessage()).isEqualTo(null);
    }

    @DisplayName("정률제 할인 쿠폰 사용시 최대 할인 금액이 별도로 지정되지 않았으면 최대 할인 한도 없이 할인된다.")
    @Test
    void shouldApplyFullPercentageDiscount_whenNoMaxDiscountIsSet() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        Coupon coupon = createCoupon(couponId, DiscountType.PERCENTAGE, null, null, 10L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(27000));
        assertThat(response.getMessage()).isEqualTo(null);
    }

    @DisplayName("정률제 할인 쿠폰 사용시 최대 할인 금액이 지정되어있으면 해당 금액 이상 할인될시 최대 할인 금액이 최종적으로 할인된다.")
    @Test
    void shouldCapDiscountAtMaxLimit_whenPercentageDiscountExceedsMax() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        Coupon coupon = createCoupon(couponId, DiscountType.PERCENTAGE, BigDecimal.valueOf(2000), null, 10L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(28000));
        assertThat(response.getMessage()).isEqualTo(null);
    }

    @DisplayName("정률제 할인 쿠폰 사용시 최대 할인 금액이 지정되어있으면 해당 금액 이하로 할인될시 할인 금액이 최종적으로 할인된다.")
    @Test
    void shouldApplyCalculatedPercentageDiscount_whenLessThanOrEqualToMaxLimit() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        Coupon coupon = createCoupon(couponId, DiscountType.PERCENTAGE, BigDecimal.valueOf(5000), null, 10L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(27000));
        assertThat(response.getMessage()).isEqualTo(null);
    }

    @DisplayName("여러 개의 쿠폰(정액 + 정률)을 동시에 사용시 할인 금액이 합산되어 계산된다.")
    @Test
    void shouldCalculateTotalDiscount_whenUsingMultipleCoupons() {
        //given
        UUID userId = UUID.randomUUID();
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        UUID couponId1 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, BigDecimal.valueOf(3000), 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId2 = 2L;
        Coupon coupon2 = createCoupon(couponId2, DiscountType.PERCENTAGE, BigDecimal.valueOf(5000), null, 10L);
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        BigDecimal expectedDiscount = BigDecimal.valueOf(6000);
        BigDecimal expectedFinalAmount = BigDecimal.valueOf(24000);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(expectedDiscount);
        assertThat(response.getFinalAmount()).isEqualByComparingTo(expectedFinalAmount);
        assertThat(response.getMessage()).isNull();
    }

    @DisplayName("여러 개의 쿠폰 사용시 총 할인 금액이 주문 금액을 초과하면 최종 결제 금액은 0원이 된다.")
    @Test
    void shouldReturnZeroFinalAmount_whenTotalDiscountExceedsOrderAmount() {
        //given
        UUID userId = UUID.randomUUID();
        BigDecimal orderAmount = BigDecimal.valueOf(5000);

        UUID couponId1 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, BigDecimal.valueOf(3000), 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId2 = 2L;
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, BigDecimal.valueOf(3000), 0L);
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(6000));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getMessage()).isNull();
    }

    @DisplayName("쿠폰이 여러개일 때 하나라도 존재하지 않는 쿠폰으로 쿠폰 사용 요청시 실패한다.")
    @Test
    void shouldFail_whenCouponDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId1, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");
    }

    @DisplayName("쿠폰이 한 개일 때 존재하지 않는 쿠폰으로 쿠폰 사용 요청시 실패한다.")
    @Test
    void shouldFail_whenSingleCouponNotFound() {
        //given
        UUID userId = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of());

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("요청한 쿠폰 중 존재하지 않는 쿠폰이 포함되어 있습니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 내가 소유하지 않은 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenUserTriesToUseCouponTheyDoNotOwn() {
        //given
        UUID userId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, nonOwnerId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("본인의 쿠폰만 사용할 수 있습니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 내가 소유하지 않은 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenUsingMultipleCouponsAndOneIsNotOwned() {
        //given
        UUID userId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, nonOwnerId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("본인의 쿠폰만 사용할 수 있습니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 이미 사용한 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenUserTriesToUseAlreadyUsedCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.USED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("이미 사용한 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 이미 사용한 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenAnyOfMultipleCouponsIsAlreadyUsed() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.USED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("이미 사용한 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 만료된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenUsingExpiredCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.EXPIRED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("만료된 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 만료된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenAnyOfMultipleCouponsIsExpired() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.EXPIRED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("만료된 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 ISSUED 상태의 쿠폰이라도 기간이 지나 만료된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenIssuedCouponIsExpiredByDate() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().minusMinutes(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("만료된 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 ISSUED 상태의 쿠폰이라도 기간이 지나 만료된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenAnyIssuedCouponIsExpiredByDateAmongMultipleCoupons() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().minusMinutes(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("만료된 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 회수된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenUserTriesToUseRevokedCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.REVOKED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("사용이 불가능한 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 회수된 쿠폰으로 쿠폰 사용을 요청하면 실패한다.")
    @Test
    void shouldFail_whenAnyOfMultipleCouponsIsRevoked() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.REVOKED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("사용이 불가능한 쿠폰입니다.");
    }

    @DisplayName("요청한 쿠폰이 한 개일 때 주문 금액이 최소 주문 금액을 만족하지 못한 경우 할인 적용에 실패한다.")
    @Test
    void shouldFail_whenOrderAmountIsLessThanMinimumRequired() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 1L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon = createCoupon(couponId, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L, BigDecimal.valueOf(10000000));
        IssuedCoupon issuedCoupon = createIssuedCoupon(issuedCouponId, userId, couponId, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("최소 주문 금액을 만족하지 못했습니다.");
    }

    @DisplayName("요청한 쿠폰이 여러 개일 때 하나라도 주문 금액이 최소 주문 금액을 만족하지 못한 경우 할인 적용에 실패한다.")
    @Test
    void shouldFail_whenAnyOfMultipleCouponsDoesNotMeetMinOrderAmount() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();
        Long issuedCouponId1 = 1L;
        Long issuedCouponId2 = 2L;
        BigDecimal orderAmount = BigDecimal.valueOf(30000);
        BigDecimal discountAmount = BigDecimal.valueOf(3000);

        Coupon coupon1 = createCoupon(couponId1, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L, BigDecimal.valueOf(10000000));
        Coupon coupon2 = createCoupon(couponId2, DiscountType.FIXED_AMOUNT, null, discountAmount, 0L);
        IssuedCoupon issuedCoupon1 = createIssuedCoupon(issuedCouponId1, userId, couponId1, IssuedCouponStatus.ISSUED,
                LocalDateTime.now().plusDays(1));
        IssuedCoupon issuedCoupon2 = createIssuedCoupon(issuedCouponId2, userId, couponId2, IssuedCouponStatus.REVOKED,
                LocalDateTime.now().plusDays(1));

        CouponValidationRequest request = createRequest(userId, List.of(issuedCouponId1, issuedCouponId2), orderAmount);

        given(issuedCouponRepository.findAllById(anyList())).willReturn(List.of(issuedCoupon1, issuedCoupon2));
        given(couponRepository.findAllById(anyList())).willReturn(List.of(coupon1, coupon2));

        //when
        CouponValidationResponse response = couponValidationService.validateAndCalculate(request);

        //then
        assertThat(response.isValid()).isFalse();
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getFinalAmount()).isNull();
        assertThat(response.getMessage()).isEqualTo("최소 주문 금액을 만족하지 못했습니다.");
    }

    private CouponValidationRequest createRequest(UUID userId, List<Long> ids, BigDecimal totalAmount) {
        return CouponValidationRequest.builder()
                .customerId(userId)
                .couponIds(ids)
                .totalOrderAmount(totalAmount)
                .sagaId(UUID.randomUUID())
                .build();
    }

    private Coupon createCoupon(UUID id, DiscountType type, BigDecimal maxDiscount, BigDecimal amountOff,
                                Long percentOff) {
        return createCoupon(id, type, maxDiscount, amountOff, percentOff, BigDecimal.ZERO);
    }

    private Coupon createCoupon(UUID id, DiscountType type, BigDecimal maxDiscount, BigDecimal amountOff,
                                Long percentOff, BigDecimal minDiscountAmount) {
        return Coupon.builder()
                .couponId(id)
                .discountType(type)
                .maxDiscountAmount(maxDiscount)
                .amountOff(amountOff)
                .percentOff(percentOff)
                .minDiscountAmount(minDiscountAmount)
                .build();
    }

    private IssuedCoupon createIssuedCoupon(Long id, UUID userId, UUID couponId, IssuedCouponStatus status,
                                            LocalDateTime expiredAt) {
        return IssuedCoupon.builder()
                .id(id)
                .userId(userId)
                .couponId(couponId)
                .status(status)
                .expiredAt(expiredAt)
                .build();
    }

}
