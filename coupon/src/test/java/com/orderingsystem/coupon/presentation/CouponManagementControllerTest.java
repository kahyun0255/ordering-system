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
import com.orderingsystem.coupon.infra.redis.RedisCouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;

class CouponManagementControllerTest extends ControllerTestSupport {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private RedisCouponRepository redisCouponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${coupon.redis.issued-prefix}")
    private String couponIssueKey;

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
    }

    @DisplayName("관리자는 쿠폰을 정지시킬 수 있다.")
    @Test
    void shouldDeactivateCoupon_whenUserHasAdminRole() throws Exception {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.ACTIVE)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .build();

        couponRepository.save(coupon);

        redisCouponRepository.enableCoupon(coupon.getCouponId(), 1000L, LocalDateTime.now());
        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();

        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/pause")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.PAUSED);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();
    }

    @DisplayName("관리자가 아닌 유저는 쿠폰을 정지시킬 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldReturn403_whenUserWithoutAdminRoleTriesToDeactivateCoupon(String role, UserType userType)
            throws Exception {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.ACTIVE)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .build();

        couponRepository.save(coupon);

        redisCouponRepository.enableCoupon(coupon.getCouponId(), 1000L, LocalDateTime.now());
        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();

        String token = buildToken(UUID.randomUUID(), userType);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/pause")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("쿠폰 정지가 불가능합니다."));

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.ACTIVE);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();
    }

    @DisplayName("쿠폰을 정지하려할 때 쿠폰이 존재하지 않으면 404를 반환한디.")
    @Test
    void shouldReturn404_whenCouponToDeactivateDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when, then
        mockMvc.perform(
                        post("/api/coupons/" + UUID.randomUUID() + "/pause")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("쿠폰이 존재하지 않습니다."));
    }

    @DisplayName("관리자는 정지된 쿠폰을 재시작할 수 있다.")
    @Test
    void shouldReactivateCoupon_whenUserHasAdminRoleAndCouponIsSuspended() throws Exception {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.PAUSED)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .issueLimit(100L)
                .issuedCount(10L)
                .build();

        couponRepository.save(coupon);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();

        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/resume")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.ACTIVE);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();
        assertThat(redisCouponRepository.currentStock(coupon.getCouponId())).isEqualTo(90L);
    }

    @DisplayName("관리자가 아니라면 정지된 쿠폰을 재시작할 수 없고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldReturn403_whenNonAdminUserTriesToReactivateSuspendedCoupon(String role, UserType userType)
            throws Exception {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.PAUSED)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .issueLimit(100L)
                .issuedCount(10L)
                .build();

        couponRepository.save(coupon);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();

        String token = buildToken(UUID.randomUUID(), userType);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/resume")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("쿠폰 재시작이 불가능합니다."));

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.PAUSED);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();
    }

    @DisplayName("PAUSE 상태가 아닌 쿠폰은 재시작이 불가능하고 400을 반환한다.")
    @ParameterizedTest(name = "[{index}] 쿠폰 상태 : {0}")
    @MethodSource("provideNonPauseStatuses")
    void shouldReturn400_whenTryingToReactivateCouponWithInvalidStatus(String status, CouponStatus couponStatus)
            throws Exception {
        //given
        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(couponStatus)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .issueLimit(100L)
                .issuedCount(10L)
                .build();

        couponRepository.save(coupon);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();

        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/resume")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("정지된 쿠폰만 재시작할 수 있습니다."));

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(couponStatus);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();
    }

    private static Stream<Arguments> provideNonPauseStatuses() {
        return Stream.of(
                Arguments.of("활성화된 쿠폰", CouponStatus.ACTIVE),
                Arguments.of("활성화 대기중", CouponStatus.SCHEDULED),
                Arguments.of("만료된 쿠폰", CouponStatus.EXPIRED),
                Arguments.of("보관된 쿠폰", CouponStatus.ARCHIVED)
        );
    }

    @DisplayName("재시작할 쿠폰이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenCouponToReactivateDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when, then
        mockMvc.perform(
                        post("/api/coupons/" + UUID.randomUUID() + "/resume")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("쿠폰이 존재하지 않습니다."));
    }

    @DisplayName("관리자는 정지된 쿠폰을 종료할 수 있다.")
    @Test
    void shouldExpireCoupon_whenUserIsAdminAndCouponIsPaused() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.PAUSED)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .issueLimit(100L)
                .issuedCount(10L)
                .build();

        couponRepository.save(coupon);

        redisCouponRepository.enableCoupon(coupon.getCouponId(), coupon.getIssueLimit(), null);
        redisCouponRepository.addIssuedUser(coupon.getCouponId(), userId);
        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();

        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/terminate")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.ARCHIVED);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isFalse();

        String issuedKey = couponIssueKey + coupon.getCouponId();
        Long ttl = redisTemplate.getExpire(issuedKey);
        assertThat(ttl).isGreaterThan(0L);
        assertThat(ttl).isLessThanOrEqualTo(604800L);
    }

    @DisplayName("관리자가 아니라면 쿠폰을 종료할 수 없고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 유저 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldReturn403_whenNonAdminUserTriesToExpireSuspendedCoupon(String role, UserType userType)
            throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Coupon coupon = Coupon.builder()
                .couponId(UUID.randomUUID())
                .name("쿠폰")
                .status(CouponStatus.ACTIVE)
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(1000))
                .validFrom(LocalDateTime.of(2025, 12, 14, 0, 0))
                .issueLimit(100L)
                .issuedCount(10L)
                .build();

        couponRepository.save(coupon);

        redisCouponRepository.enableCoupon(coupon.getCouponId(), coupon.getIssueLimit(), null);
        redisCouponRepository.addIssuedUser(coupon.getCouponId(), userId);
        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();

        String token = buildToken(UUID.randomUUID(), userType);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + coupon.getCouponId() + "/terminate")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("쿠폰 종료가 불가능합니다."));

        //then
        Optional<Coupon> after = couponRepository.findById(coupon.getCouponId());
        assertThat(after).isPresent();
        assertThat(after.get().getStatus()).isEqualTo(CouponStatus.ACTIVE);

        assertThat(redisCouponRepository.exists(coupon.getCouponId())).isTrue();

        String issuedKey = couponIssueKey + coupon.getCouponId();
        Long ttl = redisTemplate.getExpire(issuedKey);
        assertThat(ttl).isEqualTo(-1L);
    }

    @DisplayName("종료할 쿠폰이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenCouponToExpireDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), UserType.ADMIN);

        //when, then
        mockMvc.perform(
                        post("/api/coupons/" + UUID.randomUUID() + "/terminate")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("쿠폰이 존재하지 않습니다."));
    }

    private static Stream<Arguments> provideNonAdminRoles() {
        return Stream.of(
                Arguments.of("구매자", UserType.CUSTOMER),
                Arguments.of("레스토랑 소유자", UserType.RESTAURANT_OWNER)
        );
    }

}
