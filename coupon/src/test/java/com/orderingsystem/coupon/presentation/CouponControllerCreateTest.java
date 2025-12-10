package com.orderingsystem.coupon.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.presentation.request.CreateCouponRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CouponControllerCreateTest extends ControllerTestSupport {

    @Autowired
    private CouponRepository couponRepository;

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
    }

    @DisplayName("관리자 권한을 가진 유저가 쿠폰을 생성하면 쿠폰이 생성된다.")
    @Test
    void shouldCreateCoupon_whenUserHasAdminRole() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        //then
        List<Coupon> coupons = couponRepository.findAll();
        assertThat(coupons.size()).isEqualTo(1);

        Coupon coupon = coupons.get(0);
        assertThat(coupon.getDiscountType()).isEqualTo(request.getDiscountType());
        assertThat(coupon.getAmountOff().compareTo(request.getAmountOff())).isZero();
        assertThat(coupon.getMinDiscountAmount().compareTo(request.getMinDiscountAmount())).isZero();
        assertThat(coupon.getValidFrom()).isEqualTo(request.getValidFrom());
        assertThat(coupon.getValidUntil()).isEqualTo(request.getValidUntil());
        assertThat(coupon.getIssueLimit()).isEqualTo(request.getIssueLimit());
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.SCHEDULED);
        assertThat(coupon.getName()).isEqualTo(request.getName());
        assertThat(coupon.getIssuedCount()).isEqualTo(0L);
    }

    @DisplayName("관리자가 아닌 유저가 쿠폰 생성시 쿠폰 생성이 불가능하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldReturn403_whenUserIsNotAdmin(String type, UserType userType) throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, userType);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("쿠폰 생성이 불가능합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    private static Stream<Arguments> provideNonAdminRoles() {
        return Stream.of(
                Arguments.of("레스토랑 소유자", UserType.RESTAURANT_OWNER),
                Arguments.of("구매자", UserType.CUSTOMER)
        );
    }

    @DisplayName("쿠폰 타입이 비어있으면 쿠폰 생성이 불가능하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenCouponTypeIsMissing() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .amountOff(BigDecimal.valueOf(2000))
                .name("쿠폰")
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("discountType: 쿠폰 타입은 필수입니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("FIXED_AMOUNT 타입이며 할인 금액이 음수라면 쿠폰 생성이 불가능하다.")
    @Test
    void shouldReturn400_whenAmountOffIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(-1))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amountOff: 할인 금액은 0 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("FIXED_AMOUNT 타입이며 최소 주문 금액이 음수라면 쿠폰 생성이 불가능하다.")
    @Test
    void shouldReturn400_whenMinOrderAmountIsNegativeAndTypeIsFixedAmount() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(-1))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("minDiscountAmount: 최소 주문 금액은 0 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("쿠폰 발행 시작 시간이 비어있으면 쿠폰 생성이 불가능하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenValidFromIsMissing() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(null)
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("validFrom: 쿠폰 발행 시작 시간은 필수입니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("PERCENTAGE 타입이며 percentOff가 비어있으면 400을 반환한다.")
    @Test
    void shouldReturn400_whenPercentageTypeButPercentOffMissing() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.PERCENTAGE)
                .name("쿠폰")
                .amountOff(null)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "percentRuleOk: 타입이 PERCENTAGE면 percentOff가 필수이며 amountOff는 비워야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("PERCENTAGE 타입이며 percentOff가 0이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenPercentOffIsZero() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.PERCENTAGE)
                .name("쿠폰")
                .percentOff(0L)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("percentOff: 할인율은 1 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("PERCENTAGE 타입이며 percentOff가 101이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenPercentOffIsOver100() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.PERCENTAGE)
                .name("쿠폰")
                .percentOff(101L)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("percentOff: 할인율은 100 이하여야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("PERCENTAGE 타입인데 amountOff가 채워지면 400을 반환한다.")
    @Test
    void shouldReturn400_whenPercentageTypeButAmountOffPresent() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.PERCENTAGE)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(1000))
                .percentOff(10L)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "percentRuleOk: 타입이 PERCENTAGE면 percentOff가 필수이며 amountOff는 비워야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("FIXED_AMOUNT 타입인데 amountOff가 비어있으면 400을 반환한다.")
    @Test
    void shouldReturn400_whenFixedAmountTypeButAmountOffMissing() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(null)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "amountRuleOk: 쿠폰 타입이 FIXED_AMOUNT면 amountOff가 필수이며 percentOff, maxDiscountAmount는 비워야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("FIXED_AMOUNT 타입인데 percentOff가 채워지면 400을 반환한다.")
    @Test
    void shouldReturn400_whenFixedAmountTypeButPercentOffPresent() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .percentOff(10L)
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "amountRuleOk: 쿠폰 타입이 FIXED_AMOUNT면 amountOff가 필수이며 percentOff, maxDiscountAmount는 비워야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("FIXED_AMOUNT 타입인데 maxDiscountAmount가 채워지면 400을 반환한다.")
    @Test
    void shouldReturn400_whenFixedAmountTypeButMaxDiscountAmountPresent() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .maxDiscountAmount(BigDecimal.valueOf(5000)) // 금지
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "amountRuleOk: 쿠폰 타입이 FIXED_AMOUNT면 amountOff가 필수이며 percentOff, maxDiscountAmount는 비워야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("최대 할인 금액이 음수면 400을 반환한다.")
    @Test
    void shouldReturn400_whenMaxDiscountAmountIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.PERCENTAGE)
                .name("쿠폰")
                .percentOff(10L)
                .maxDiscountAmount(BigDecimal.valueOf(-1))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("maxDiscountAmount: 최대 할인 금액은 0 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("최소 주문 금액이 음수면 400을 반환한다.")
    @Test
    void shouldReturn400_whenMinDiscountAmountIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(-1))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("minDiscountAmount: 최소 주문 금액은 0 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("발행 개수가 음수면 400을 반환한다.")
    @Test
    void shouldReturn400_whenIssueLimitIsNegative() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 20, 0, 0))
                .issueLimit(-1L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("issueLimit: 발행 개수는 0 이상이어야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("유효기간 종료가 시작 이전이면 400을 반환한다.")
    @Test
    void shouldReturn400_whenValidUntilIsBeforeValidFrom() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("쿠폰")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 3, 23, 59))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("periodValid: 유효기간 종료는 시작 이후여야 합니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("쿠폰 이름이 비어있으면 쿠폰 생성에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenCouponNameIsBlank() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .name("")
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 5, 23, 59))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 쿠폰 이름은 필수입니다."));

        assertThat(couponRepository.count()).isZero();
    }

    @DisplayName("쿠폰 이름이 null이면 쿠폰 생성에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenCouponNameIsNull() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId, UserType.ADMIN);

        CreateCouponRequest request = CreateCouponRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025, 12, 4, 0, 0))
                .validUntil(LocalDateTime.of(2025, 12, 5, 23, 59))
                .issueLimit(10000L)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/coupons")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 쿠폰 이름은 필수입니다."));

        assertThat(couponRepository.count()).isZero();
    }

}
