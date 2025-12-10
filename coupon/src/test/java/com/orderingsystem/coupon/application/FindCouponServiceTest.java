package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;

import com.orderingsystem.coupon.application.dto.response.IssuedCouponResponse;
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

        IssuedCoupon issuedCoupon1 = IssuedCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(couponId1)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 0, 0))
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .id(2L)
                .userId(userId)
                .couponId(couponId2)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 0, 0))
                .build();

        List<IssuedCoupon> issuedCoupons = List.of(issuedCoupon1, issuedCoupon2);
        given(issuedCouponRepository.findByUserIdAndStatusIn(userId, couponStatus)).willReturn(issuedCoupons);

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
                                coupon1.getPercentOff(), coupon1.getMaxDiscountAmount(), coupon1.getMinDiscountAmount()),
                        tuple(coupon2.getCouponId(), coupon2.getName(), issuedCoupon2.getId(),
                                issuedCoupon2.getStatus(), issuedCoupon2.getIssuedAt(), issuedCoupon2.getUsedAt(),
                                issuedCoupon2.getExpiredAt(), coupon2.getDiscountType(), coupon2.getAmountOff(),
                                coupon2.getPercentOff(), coupon2.getMaxDiscountAmount(), coupon2.getMinDiscountAmount())
                );
    }

}
