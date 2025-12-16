package com.orderingsystem.coupon.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.orderingsystem.coupon.application.dto.response.CouponResponse;
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
import java.time.format.DateTimeFormatter;
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
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(10))
                .issueLimit(1000L)
                .build();

        Coupon coupon2 = Coupon.builder()
                .couponId(couponId2)
                .name("쿠폰2")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.SCHEDULED)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(LocalDateTime.now().minusMinutes(1))
                .issueLimit(1000L)
                .build();

        Coupon coupon3 = Coupon.builder()
                .couponId(couponId3)
                .name("쿠폰3")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.ARCHIVED)
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(3000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.now().plusDays(1))
                .validUntil(LocalDateTime.now().plusDays(10))
                .issueLimit(1000L)
                .build();

        Coupon coupon4 = Coupon.builder()
                .couponId(couponId4)
                .name("다른 사용자가 발급한 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .status(CouponStatus.PAUSED)
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
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {
                });

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
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {
                });

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
                objectMapper.readValue(json, new TypeReference<List<IssuedCouponResponse>>() {
                });

        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponName", "issuedCouponStatus")
                .containsExactlyInAnyOrder(
                        tuple(couponId2, "쿠폰2", IssuedCouponStatus.EXPIRED),
                        tuple(couponId3, "쿠폰3", IssuedCouponStatus.USED)
                );

    }

    @DisplayName("쿠폰 상태(status)에 따라 사용자가 발급받은 쿠폰을 필터링 조회할 수 있다.")
    @Test
    void shouldFilterUserCouponsByStatus_whenValidStatusesAreProvided() throws Exception {
        //given
        String token = buildToken(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "ACTIVE")
                                .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        List<CouponResponse> responses =
                objectMapper.readValue(json, new TypeReference<List<CouponResponse>>() {
                });

        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponName", "couponStatus")
                .containsExactlyInAnyOrder(
                        tuple(couponId1, "쿠폰1", CouponStatus.ACTIVE),
                        tuple(couponId2, "쿠폰2", CouponStatus.SCHEDULED)
                );
    }

    @DisplayName("쿠폰 조회시 상태를 지정하지 않으면 ACTIVE 상태의 쿠폰을 조회한다.")
    @Test
    void shouldRetrieveOnlyActiveCoupons_whenStatusIsNotProvided() throws Exception {
        //given
        String token = buildToken(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();

        List<CouponResponse> responses =
                objectMapper.readValue(json, new TypeReference<List<CouponResponse>>() {
                });

        assertThat(responses).hasSize(1)
                .extracting(
                        "couponId", "couponName", "discountType", "couponStatus", "amountOff", "percentOff",
                        "maxDiscountAmount", "minDiscountAmount", "validDays", "issueLimit",
                        "issuedCount"
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                couponId1,
                                "쿠폰1",
                                DiscountType.FIXED_AMOUNT,
                                CouponStatus.ACTIVE,
                                new BigDecimal("1000.00"),
                                null,
                                null,
                                new BigDecimal("10000.00"),
                                null,
                                1000L,
                                null
                        )
                );
    }

    @DisplayName("쿠폰 id로 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveCouponByCouponCode_whenValidCodeProvided() throws Exception {
        //given
        String token = buildToken(userId);

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
                .build();

        couponRepository.save(coupon);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        //when, then
        mockMvc.perform(
                        get("/api/coupons/" + coupon.getCouponId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponId").value(coupon.getCouponId().toString()))
                .andExpect(jsonPath("$.couponName").value(coupon.getName()))
                .andExpect(jsonPath("$.discountType").value(coupon.getDiscountType().toString()))
                .andExpect(jsonPath("$.couponStatus").value(coupon.getStatus().toString()))
                .andExpect(jsonPath("$.amountOff").value(coupon.getAmountOff().doubleValue()))
                .andExpect(jsonPath("$.percentOff").value(coupon.getPercentOff()))
                .andExpect(jsonPath("$.maxDiscountAmount").value(coupon.getMaxDiscountAmount()))
                .andExpect(jsonPath("$.minDiscountAmount").value(coupon.getMinDiscountAmount().doubleValue()))
                .andExpect(jsonPath("$.validFrom").value(coupon.getValidFrom().format(formatter)))
                .andExpect(jsonPath("$.validUntil").value(coupon.getValidUntil().format(formatter)))
                .andExpect(jsonPath("$.validDays").value(coupon.getValidDays()))
                .andExpect(jsonPath("$.issueLimit").value(coupon.getIssueLimit()))
                .andExpect(jsonPath("$.issuedCount").value(coupon.getIssuedCount()));
    }

    @DisplayName("쿠폰 id로 쿠폰 정보 조회시 쿠폰이 없으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenCouponCodeIsInvalid() throws Exception {
        //given
        String token = buildToken(userId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        //when, then
        mockMvc.perform(
                        get("/api/coupons/" + UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("쿠폰이 존재하지 않습니다."));
    }

    @DisplayName("발급된 쿠폰 id로 발급한 쿠폰 정보를 조회할 수 있다.")
    @Test
    void shouldRetrieveIssuedCouponById_whenValidIdProvided() throws Exception {
        //given
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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
                .build();

        couponRepository.save(coupon);

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(coupon.getCouponId())
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now().withNano(0))
                .expiredAt(LocalDateTime.now().plusDays(1).withNano(0))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/coupons/issued/" + savedIssuedCoupon.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponId").value(coupon.getCouponId().toString()))
                .andExpect(jsonPath("$.couponName").value(coupon.getName()))
                .andExpect(jsonPath("$.issuedCouponId").value(issuedCoupon.getId().toString()))
                .andExpect(jsonPath("$.issuedCouponStatus").value(issuedCoupon.getStatus().toString()))
                .andExpect(jsonPath("$.issuedAt").value(issuedCoupon.getIssuedAt().format(formatter)))
                .andExpect(jsonPath("$.usedAt").value(issuedCoupon.getUsedAt()))
                .andExpect(jsonPath("$.expiredAt").value(issuedCoupon.getExpiredAt().format(formatter)))
                .andExpect(jsonPath("$.discountType").value(coupon.getDiscountType().toString()))
                .andExpect(jsonPath("$.amountOff").value(coupon.getAmountOff().doubleValue()))
                .andExpect(jsonPath("$.percentOff").value(coupon.getPercentOff()))
                .andExpect(jsonPath("$.maxDiscountAmount").value(coupon.getMaxDiscountAmount()))
                .andExpect(jsonPath("$.minOrderAmount").value(coupon.getMinDiscountAmount().doubleValue()));
    }

    @DisplayName("발급된 쿠폰 id로 발급한 쿠폰 정보를 조회시 쿠폰이 없으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenIssuedCouponIdIsInvalid() throws Exception {
        //given
        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(userId)
                .couponId(UUID.randomUUID())
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now().withNano(0))
                .expiredAt(LocalDateTime.now().plusDays(1).withNano(0))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/coupons/issued/" + savedIssuedCoupon.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("쿠폰이 존재하지 않습니다."));
    }

    @DisplayName("발급된 쿠폰 id로 발급한 쿠폰 정보를 조회시 발급된 쿠폰 정보가 없으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenIssuedCouponNotFoundById() throws Exception {
        //given
        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/coupons/issued/" + 10000)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("발급한 쿠폰 정보가 존재하지 않습니다."));
    }

    @DisplayName("발급된 쿠폰 id로 발급한 쿠폰 정보 조회시 본인이 발급한 쿠폰이 아니라면 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserTriesToAccessOthersIssuedCoupon() throws Exception {
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
                .build();

        couponRepository.save(coupon);

        IssuedCoupon issuedCoupon = IssuedCoupon.builder()
                .userId(UUID.randomUUID())
                .couponId(coupon.getCouponId())
                .status(IssuedCouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now().withNano(0))
                .expiredAt(LocalDateTime.now().plusDays(1).withNano(0))
                .build();

        IssuedCoupon savedIssuedCoupon = issuedCouponRepository.save(issuedCoupon);

        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        get("/api/coupons/issued/" + savedIssuedCoupon.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("발급한 쿠폰 정보를 조회할 권한이 없습니다."));
    }

    @DisplayName("ACTIVE 상태 조회 시, DB상 ACTIVE라도 유효기간이 지난 쿠폰은 결과에서 제외된다.")
    @Test
    void shouldExcludeExpiredActiveCoupons_whenActiveStatusIsRequested() throws Exception {
        //given
        couponRepository.deleteAllInBatch();

        String token = buildToken(userId);
        LocalDateTime now = LocalDateTime.now();

        Coupon realActive = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("진짜 유효한 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(now.minusDays(1))
                .validUntil(now.plusDays(1))
                .issueLimit(100L)
                .build();

        Coupon hiddenExpired = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("기간 지난 활성 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(now.minusDays(5))
                .validUntil(now.minusDays(1))
                .issueLimit(100L)
                .build();

        couponRepository.saveAll(List.of(realActive, hiddenExpired));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        List<CouponResponse> responses = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(responses).hasSize(1)
                .extracting("couponId", "couponName", "couponStatus")
                .containsExactly(
                        tuple(realActive.getCouponId(), "진짜 유효한 쿠폰", CouponStatus.ACTIVE)
                );

        assertThat(responses).extracting("couponId")
                .doesNotContain(hiddenExpired.getCouponId());
    }

    @DisplayName("EXPIRED 상태 조회 시, DB상 EXPIRED인 쿠폰과 ACTIVE지만 유효기간이 지난 쿠폰이 모두 조회된다.")
    @Test
    void shouldIncludeHiddenExpiredCoupons_whenExpiredStatusIsRequested() throws Exception {
        //given
        couponRepository.deleteAllInBatch();

        String token = buildToken(userId);
        LocalDateTime now = LocalDateTime.now();

        Coupon realExpired = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("이미 만료된 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.EXPIRED)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(now.minusDays(10))
                .validUntil(now.minusDays(5))
                .issueLimit(100L)
                .build();

        Coupon hiddenExpired = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("기간 지난 활성 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(now.minusDays(5))
                .validUntil(now.minusDays(1))
                .issueLimit(100L)
                .build();

        Coupon realActive = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("지금 유효한 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(now.minusDays(1))
                .validUntil(now.plusDays(1))
                .issueLimit(100L)
                .build();

        couponRepository.saveAll(List.of(realExpired, hiddenExpired, realActive));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "EXPIRED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        List<CouponResponse> responses = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponName", "couponStatus")
                .containsExactlyInAnyOrder(
                        tuple(realExpired.getCouponId(), "이미 만료된 쿠폰", CouponStatus.EXPIRED),
                        tuple(hiddenExpired.getCouponId(), "기간 지난 활성 쿠폰", CouponStatus.EXPIRED)
                );
    }

    @DisplayName("ACTIVE와 PAUSED 상태를 함께 조회하면, 유효기간이 지난 ACTIVE는 제외되고 조회된다.")
    @Test
    void shouldRetrieveActiveAndPausedCoupons_excludingHiddenExpired() throws Exception {
        //given
        couponRepository.deleteAllInBatch();
        String token = buildToken(userId);
        LocalDateTime now = LocalDateTime.now();

        Coupon realActive = createCoupon(CouponStatus.ACTIVE, now.plusDays(1));
        Coupon paused = createCoupon(CouponStatus.PAUSED, now.plusDays(1));
        Coupon hiddenExpired = createCoupon(CouponStatus.ACTIVE, now.minusDays(1));
        Coupon realExpired = createCoupon(CouponStatus.EXPIRED, now.minusDays(5));

        couponRepository.saveAll(List.of(realActive, paused, hiddenExpired, realExpired));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "ACTIVE")
                                .param("status", "PAUSED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        List<CouponResponse> responses = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(responses).hasSize(2)
                .extracting("couponId", "couponStatus")
                .containsExactlyInAnyOrder(
                        tuple(realActive.getCouponId(), CouponStatus.ACTIVE),
                        tuple(paused.getCouponId(), CouponStatus.PAUSED)
                );

        assertThat(responses).extracting("couponId")
                .doesNotContain(hiddenExpired.getCouponId());
    }

    @DisplayName("EXPIRED와 SCHEDULED 상태를 함께 조회하면, 숨겨진 만료 쿠폰도 포함되어 조회된다.")
    @Test
    void shouldRetrieveExpiredAndScheduledCoupons_includingHiddenExpired() throws Exception {
        //given
        couponRepository.deleteAllInBatch();
        String token = buildToken(userId);
        LocalDateTime now = LocalDateTime.now();

        Coupon realExpired = createCoupon(CouponStatus.EXPIRED, now.minusDays(5));
        Coupon hiddenExpired = createCoupon(CouponStatus.ACTIVE, now.minusDays(1));
        Coupon scheduled = createCoupon(CouponStatus.SCHEDULED, now.plusDays(10));
        Coupon realActive = createCoupon(CouponStatus.ACTIVE, now.plusDays(1));

        couponRepository.saveAll(List.of(realExpired, hiddenExpired, scheduled, realActive));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .param("status", "EXPIRED")
                                .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        List<CouponResponse> responses = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(responses).hasSize(3)
                .extracting("couponId", "couponStatus")
                .containsExactlyInAnyOrder(
                        tuple(realExpired.getCouponId(), CouponStatus.EXPIRED),
                        tuple(hiddenExpired.getCouponId(), CouponStatus.EXPIRED),
                        tuple(scheduled.getCouponId(), CouponStatus.SCHEDULED)
                );
    }

    @DisplayName("쿠폰 id로 쿠폰 조회 시, DB에서 ACTIVE 상태라도 유효기간이 지났다면 EXPIRED 상태로 반환한다.")
    @Test
    void shouldReturnExpiredStatus_whenRetrievingHiddenExpiredCoupon() throws Exception {
        //given
        String token = buildToken(userId);

        LocalDateTime past = LocalDateTime.now().minusDays(1);

        Coupon hiddenExpiredCoupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("기간 지난 활성 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(CouponStatus.ACTIVE)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(past.minusDays(10))
                .validUntil(past)
                .issueLimit(100L)
                .build();

        couponRepository.save(hiddenExpiredCoupon);

        //when, then
        mockMvc.perform(
                        get("/api/coupons/" + hiddenExpiredCoupon.getCouponId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponId").value(hiddenExpiredCoupon.getCouponId().toString()))
                .andExpect(jsonPath("$.couponName").value(hiddenExpiredCoupon.getName()))
                .andExpect(jsonPath("$.couponStatus").value(CouponStatus.EXPIRED.toString()))
                .andExpect(jsonPath("$.validUntil").exists());
    }

    private Coupon createCoupon(CouponStatus status, LocalDateTime validUntil) {
        return Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("테스트 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .status(status)
                .amountOff(BigDecimal.valueOf(1000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.now().minusDays(10))
                .validUntil(validUntil)
                .issueLimit(100L)
                .build();
    }

}
