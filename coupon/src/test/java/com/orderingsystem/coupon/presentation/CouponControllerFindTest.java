package com.orderingsystem.coupon.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class CouponControllerFindTest extends ControllerTestSupport {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID couponId1 = UUID.randomUUID();
    private final UUID couponId2 = UUID.randomUUID();
    private final UUID couponId3 = UUID.randomUUID();
    private final UUID couponId4 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
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

        Coupon coupon3 = Coupon.builder()
                .couponId(couponId3)
                .name("쿠폰3")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.ACTIVE)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        Coupon coupon4 = Coupon.builder()
                .couponId(couponId4)
                .name("다른 사용자가 발급한 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.ACTIVE)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 10, 12, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(1000L)
                .build();

        couponRepository.saveAll(List.of(coupon1, coupon2, coupon3, coupon4));

        IssuedCoupon issuedCoupon1 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(couponId1)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 12, 12))
                .expiredAt(LocalDateTime.of(2025, 12, 20, 12, 0))
                .build();

        IssuedCoupon issuedCoupon2 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(couponId2)
                .status(IssuedCouponStatus.EXPIRED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 12, 12))
                .expiredAt(LocalDateTime.of(2025, 12, 20, 12, 0))
                .build();

        IssuedCoupon issuedCoupon3 = IssuedCoupon.builder()
                .userId(userId)
                .couponId(couponId3)
                .status(IssuedCouponStatus.USED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 12, 12))
                .expiredAt(LocalDateTime.of(2025, 12, 20, 12, 0))
                .orderId(UUID.randomUUID())
                .usedAt(LocalDateTime.of(2025, 12, 12, 12, 12))
                .build();

        IssuedCoupon issuedCoupon4 = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(couponId4)
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.of(2025, 12, 10, 12, 12))
                .expiredAt(LocalDateTime.of(2025, 12, 20, 12, 0))
                .orderId(UUID.randomUUID())
                .usedAt(LocalDateTime.of(2025, 12, 12, 12, 12))
                .build();

        issuedCouponRepository.saveAll(List.of(issuedCoupon1, issuedCoupon2, issuedCoupon3, issuedCoupon4));
    }

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
        issuedCouponRepository.deleteAllInBatch();
    }

    @DisplayName("해당 사용자가 발급한 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveIssuedCoupons_whenUserIsValid() throws Exception {
        //given
        String token = buildToken(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons/issued")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "ISSUED")
                                .param("status", "USED")
                                .param("status", "EXPIRED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        List<IssuedCouponResponse> responses =
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {});

        assertThat(responses).hasSize(3)
                .extracting("couponId", "couponName", "issuedCouponStatus")
                .containsExactlyInAnyOrder(
                        tuple(couponId1, "쿠폰1", IssuedCouponStatus.ISSUED),
                        tuple(couponId2, "쿠폰2", IssuedCouponStatus.EXPIRED),
                        tuple(couponId3, "쿠폰3", IssuedCouponStatus.USED)
                );

    }

    @DisplayName("발급한 쿠폰 정보 조회시 쿠폰 발급 상태를 지정하지 않으면 ISSUED 상태의 쿠폰만 조회한다.")
    @Test
    void shouldRetrieveOnlyIssuedCoupons_whenStatusIsNotSpecified() throws Exception {
        //given
        String token = buildToken(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons/issued")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        List<IssuedCouponResponse> responses =
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {});

        assertThat(responses).hasSize(1)
                .extracting("couponId", "couponName", "issuedCouponStatus")
                .containsExactlyInAnyOrder(
                        tuple(couponId1, "쿠폰1", IssuedCouponStatus.ISSUED)
                );

    }

    @DisplayName("발급된 쿠폰 중 특정 상태의 쿠폰만 조회할 수 있다.")
    @Test
    void shouldRetrieveCouponsByStatusFilter_whenUserIsValid() throws Exception {
        //given
        String token = buildToken(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons/issued")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "USED")
                                .param("status", "EXPIRED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        List<IssuedCouponResponse> responses =
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {});

        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponName", "issuedCouponStatus")
                .containsExactlyInAnyOrder(
                        tuple(couponId2, "쿠폰2", IssuedCouponStatus.EXPIRED),
                        tuple(couponId3, "쿠폰3", IssuedCouponStatus.USED)
                );

    }

}
