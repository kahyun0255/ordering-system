package com.orderingsystem.coupon.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.coupon.application.port.out.CouponIssueMessagePublisher;
import com.orderingsystem.coupon.domain.event.CouponIssuedEvent;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CouponControllerIssueTest extends ControllerTestSupport {

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        public RedisTransaction redisTransaction() {
            return new RedisTransaction() {
                @Override
                public void execute(RedisTemplate<String, String> redisTemplate,
                                    java.util.function.Consumer<RedisOperations<String, String>> callback) {
                    callback.accept(redisTemplate);
                }
            };
        }
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private CouponIssueMessagePublisher couponIssueMessagePublisher;

    @Value("${coupon.redis.stock-prefix}")
    private String stockPrefix;

    @Value("${coupon.redis.issued-prefix}")
    private String issuedPrefix;

    private final UUID couponId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private String stockKey(UUID couponId) {
        return stockPrefix + couponId;
    }

    private String issuedKey(UUID couponId) {
        return issuedPrefix + couponId;
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(stockKey(couponId));
    }

    @DisplayName("재고가 있고 최초 발급이면 쿠폰이 발급되고, 발급 이벤트가 발행된다.")
    @Test
    void shouldIssueCoupon_whenStockExistsAndFirstIssue() throws Exception {
        //given
        redisTemplate.opsForValue().set(stockKey(couponId), "2000");
        String token = buildToken(userId);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + couponId + "/claims")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("1999");
        assertThat(redisTemplate.opsForSet().members(issuedKey(couponId))).contains(userId.toString());

        ArgumentCaptor<CouponIssuedEvent> captor = ArgumentCaptor.forClass(CouponIssuedEvent.class);
        verify(couponIssueMessagePublisher, times(1)).publish(captor.capture());

        CouponIssuedEvent event = captor.getValue();
        assertThat(event.getCouponId()).isEqualTo(couponId);
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @DisplayName("존재하지 않는 쿠폰 발급 요청을 보내면 404를 반환한다.")
    @Test
    void shouldReturn404_whenCouponKeyDoesNotExist() throws Exception {
        //given
        String token = buildToken(userId);

        //when, then
        mockMvc.perform(
                        post("/api/coupons/" + couponId + "/claims")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 쿠폰입니다."));
    }

    @DisplayName("요청한 쿠폰을 이미 발급했으면 400을 반환하고 쿠폰 발급에 실패한다.")
    @Test
    void shouldReturn400_whenAlreadyIssued() throws Exception {
        //given
        redisTemplate.opsForValue().set(stockKey(couponId), "2000");
        redisTemplate.opsForSet().add(issuedKey(couponId), userId.toString());
        String token = buildToken(userId);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + couponId + "/claims")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("이미 쿠폰이 발급되었습니다."));

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("2000");
        assertThat(redisTemplate.opsForSet().members(issuedKey(couponId))).contains(userId.toString());

        verifyNoInteractions(couponIssueMessagePublisher);
    }

    @DisplayName("재고가 없으면 400을 반환하고 쿠폰 발급에 실패한다.")
    @Test
    void shouldReturn400_whenSoldOut() throws Exception {
        //given
        redisTemplate.opsForValue().set(stockKey(couponId), "0");
        String token = buildToken(userId);

        //when
        mockMvc.perform(
                        post("/api/coupons/" + couponId + "/claims")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("쿠폰이 마감되었습니다."));

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey(couponId))).isEqualTo("0");
        assertThat(redisTemplate.opsForSet().members(issuedKey(couponId))).doesNotContain(userId.toString());

        verifyNoInteractions(couponIssueMessagePublisher);
    }

}
