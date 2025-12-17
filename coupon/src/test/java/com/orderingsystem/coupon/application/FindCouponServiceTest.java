package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.coupon.application.dto.response.CouponResponse;
import com.orderingsystem.coupon.application.dto.response.IssuedCouponResponse;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private FindCouponService findCouponService;

    @DisplayName("해당 사용자가 발급한 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveIssuedCouponsByUserId() {
        //given
        UUID userId = UUID.randomUUID();
        List<IssuedCouponStatus> couponStatus = List.of(IssuedCouponStatus.ISSUED);

        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();

        LocalDateTime dateTime = LocalDateTime.now().plusDays(10);

        IssuedCoupon issuedCoupon1 = IssuedCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(couponId1)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(dateTime)
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .id(2L)
                .userId(userId)
                .couponId(couponId2)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(dateTime)
                .build();

        List<IssuedCoupon> issuedCoupons = List.of(issuedCoupon1, issuedCoupon2);
        given(issuedCouponRepository.findByUserIdAndStatusIn(eq(userId), anyCollection())).willReturn(issuedCoupons);

        Coupon coupon1 = Coupon.builder()
                .couponId(couponId1)
                .name("쿠폰1")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        Coupon coupon2 = Coupon.builder()
                .couponId(couponId2)
                .name("쿠폰2")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.ACTIVE)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        given(couponRepository.findAllByCouponIdIn(List.of(couponId1, couponId2))).willReturn(
                List.of(coupon1, coupon2));

        //when
        List<IssuedCouponResponse> responses = findCouponService.getIssuedCoupons(userId, couponStatus);

        //then
        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponName", "issuedCouponId", "issuedCouponStatus", "issuedAt", "usedAt",
                        "expiredAt", "discountType", "amountOff", "percentOff", "maxDiscountAmount", "minOrderAmount")
                .containsExactlyInAnyOrder(
                        tuple(coupon1.getCouponId(), coupon1.getName(), issuedCoupon1.getId(),
                                issuedCoupon1.getStatus(), issuedCoupon1.getIssuedAt(), issuedCoupon1.getUsedAt(),
                                issuedCoupon1.getExpiredAt(), coupon1.getDiscountType(), coupon1.getAmountOff(),
                                coupon1.getPercentOff(), coupon1.getMaxDiscountAmount(),
                                coupon1.getMinDiscountAmount()),
                        tuple(coupon2.getCouponId(), coupon2.getName(), issuedCoupon2.getId(),
                                issuedCoupon2.getStatus(), issuedCoupon2.getIssuedAt(), issuedCoupon2.getUsedAt(),
                                issuedCoupon2.getExpiredAt(), coupon2.getDiscountType(), coupon2.getAmountOff(),
                                coupon2.getPercentOff(), coupon2.getMaxDiscountAmount(), coupon2.getMinDiscountAmount())
                );
    }

    @DisplayName("쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveCoupons_whenUserIsValid() {
        //given
        UUID userId = UUID.randomUUID();
        List<CouponStatus> couponStatus = List.of(CouponStatus.ACTIVE);

        UUID couponId1 = UUID.randomUUID();
        UUID couponId2 = UUID.randomUUID();

        Coupon coupon1 = Coupon.builder()
                .couponId(couponId1)
                .name("쿠폰1")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        Coupon coupon2 = Coupon.builder()
                .couponId(couponId2)
                .name("쿠폰2")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.ACTIVE)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        given(couponRepository.findAllByStatusIn(anyCollection())).willReturn(List.of(coupon1, coupon2));

        //when
        List<CouponResponse> responses = findCouponService.getCoupons(userId, couponStatus);

        //then
        assertThat(responses).hasSize(2)
                .extracting(
                        "couponId", "couponName", "discountType", "couponStatus", "amountOff", "percentOff",
                        "maxDiscountAmount", "minDiscountAmount", "validFrom", "validUntil", "validDays", "issueLimit",
                        "issuedCount"
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                coupon1.getCouponId(),
                                coupon1.getName(),
                                coupon1.getDiscountType(),
                                coupon1.getStatus(),
                                coupon1.getAmountOff(),
                                coupon1.getPercentOff(),
                                coupon1.getMaxDiscountAmount(),
                                coupon1.getMinDiscountAmount(),
                                coupon1.getValidFrom(),
                                coupon1.getValidUntil(),
                                coupon1.getValidDays(),
                                coupon1.getIssueLimit(),
                                coupon1.getIssuedCount()
                        ),
                        tuple(
                                coupon2.getCouponId(),
                                coupon2.getName(),
                                coupon2.getDiscountType(),
                                coupon2.getStatus(),
                                coupon2.getAmountOff(),
                                coupon2.getPercentOff(),
                                coupon2.getMaxDiscountAmount(),
                                coupon2.getMinDiscountAmount(),
                                coupon2.getValidFrom(),
                                coupon2.getValidUntil(),
                                coupon2.getValidDays(),
                                coupon2.getIssueLimit(),
                                coupon2.getIssuedCount()
                        )
                );
    }

    @DisplayName("쿠폰 id로 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveCouponByCouponCode_whenValidCodeProvided() {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .issuedCount(10L)
                .build();

        given(couponRepository.findById(coupon.getCouponId())).willReturn(Optional.of(coupon));

        //when
        CouponResponse response = findCouponService.getCoupon(UUID.randomUUID(), coupon.getCouponId());

        //then
        assertThat(response.getCouponId()).isEqualTo(coupon.getCouponId());
        assertThat(response.getCouponName()).isEqualTo(coupon.getName());
        assertThat(response.getDiscountType()).isEqualTo(coupon.getDiscountType());
        assertThat(response.getCouponStatus()).isEqualTo(coupon.getStatus());
        assertThat(response.getAmountOff()).isEqualTo(coupon.getAmountOff());
        assertThat(response.getPercentOff()).isEqualTo(coupon.getPercentOff());
        assertThat(response.getMaxDiscountAmount()).isEqualTo(coupon.getMaxDiscountAmount());
        assertThat(response.getMinDiscountAmount()).isEqualTo(coupon.getMinDiscountAmount());
        assertThat(response.getValidFrom()).isEqualTo(coupon.getValidFrom());
        assertThat(response.getValidUntil()).isEqualTo(coupon.getValidUntil());
        assertThat(response.getValidDays()).isEqualTo(coupon.getValidDays());
        assertThat(response.getIssueLimit()).isEqualTo(coupon.getIssueLimit());
        assertThat(response.getIssuedCount()).isEqualTo(coupon.getIssuedCount());
    }

    @DisplayName("쿠폰 id로 쿠폰 조회시 해당 쿠폰이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCouponIdDoesNotExist() {
        //given
        UUID couponId = UUID.randomUUID();

        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> findCouponService.getCoupon(UUID.randomUUID(), couponId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰이 존재하지 않습니다.");
    }

    @DisplayName("발급한 쿠폰 id로 발급한 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveIssuedCouponById_whenUserIsOwner() {
        //given
        UUID userId = UUID.randomUUID();
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .issuedCount(10L)
                .build();

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .id(10L)
                .userId(userId)
                .couponId(coupon.getCouponId())
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now().withNano(0))
                .expiredAt(LocalDateTime.now().plusDays(1).withNano(0))
                .build();

        given(couponRepository.findById(coupon.getCouponId())).willReturn(Optional.of(coupon));
        given(issuedCouponRepository.findById(issuedCoupon.getId())).willReturn(Optional.of(issuedCoupon));

        //when
        IssuedCouponResponse response = findCouponService.getIssuedCoupon(userId, issuedCoupon.getId());

        //then
        assertThat(response.getCouponId()).isEqualTo(coupon.getCouponId());
        assertThat(response.getCouponName()).isEqualTo(coupon.getName());
        assertThat(response.getIssuedCouponId()).isEqualTo(issuedCoupon.getId());
        assertThat(response.getIssuedCouponStatus()).isEqualTo(issuedCoupon.getStatus());
        assertThat(response.getIssuedAt()).isEqualTo(issuedCoupon.getIssuedAt());
        assertThat(response.getUsedAt()).isEqualTo(issuedCoupon.getUsedAt());
        assertThat(response.getExpiredAt()).isEqualTo(issuedCoupon.getExpiredAt());
        assertThat(response.getDiscountType()).isEqualTo(coupon.getDiscountType());
        assertThat(response.getAmountOff()).isEqualTo(coupon.getAmountOff());
        assertThat(response.getPercentOff()).isEqualTo(coupon.getPercentOff());
        assertThat(response.getMaxDiscountAmount()).isEqualTo(coupon.getMaxDiscountAmount());
        assertThat(response.getMinOrderAmount()).isEqualTo(coupon.getMinDiscountAmount());
    }

    @DisplayName("발급한 쿠폰 정보 조회시 발급한 쿠폰 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenIssuedCouponNotFound() {
        //given
        UUID userId = UUID.randomUUID();
        Long issuedCouponId = 100L;

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> findCouponService.getIssuedCoupon(userId, issuedCouponId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("발급한 쿠폰 정보가 존재하지 않습니다.");
    }

    @DisplayName("발급한 쿠폰 정보 조회시 유저가 발급하지 않은 쿠폰 조회를 요청한 경우 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsNotOwnerOfIssuedCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        Long issuedCouponId = 100L;

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));
        given(issuedCoupon.getId()).willReturn(100L);
        given(issuedCoupon.getUserId()).willReturn(UUID.randomUUID());

        //when, then
        assertThatThrownBy(() -> findCouponService.getIssuedCoupon(userId, issuedCouponId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("발급한 쿠폰 정보를 조회할 권한이 없습니다.");
    }

    @DisplayName("발급한 쿠폰 정보 조회시 쿠폰 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCouponNotFoundFromIssuedCoupon() {
        //given
        UUID userId = UUID.randomUUID();
        UUID couponId = UUID.randomUUID();
        Long issuedCouponId = 100L;

        IssuedCoupon issuedCoupon = mock(IssuedCoupon.class);

        given(issuedCouponRepository.findById(issuedCouponId)).willReturn(Optional.of(issuedCoupon));
        given(issuedCoupon.getUserId()).willReturn(userId);
        given(issuedCoupon.getCouponId()).willReturn(couponId);
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> findCouponService.getIssuedCoupon(userId, issuedCouponId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessage("쿠폰이 존재하지 않습니다.");
    }

}
