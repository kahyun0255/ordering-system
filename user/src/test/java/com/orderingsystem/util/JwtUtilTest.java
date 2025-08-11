package com.orderingsystem.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String RAW = "test12345678901234567890test1234567890";
    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(RAW.getBytes(StandardCharsets.UTF_8));
    private static final String ISSUER = "test_issuer";


    private JwtUtil jwtUtil(Duration accessTtl, Duration refreshTtl) {
        return new JwtUtil(SECRET_KEY, accessTtl, refreshTtl, ISSUER);
    }

    @DisplayName("액세스 토큰에 올바른 클레임이 담긴다.")
    @Test
    void accessTokenContainsValidClaims() {
        //given
        JwtUtil jwtUtil = jwtUtil(Duration.ofMinutes(5), Duration.ofDays(7));
        User user = getUser();

        //when
        String accessToken = jwtUtil.createAccessToken(user);

        //then
        Claims accessTokenClaims = jwtUtil.getClaims(accessToken);
        assertThat(accessTokenClaims.getIssuer()).isEqualTo(ISSUER);
        assertThat(accessTokenClaims.getSubject()).isEqualTo(user.getUserId().toString());
        assertThat(accessTokenClaims.get("typ", String.class)).isEqualTo("access");
        assertThat(accessTokenClaims.get("userId", String.class)).isEqualTo(user.getUserId().toString());
        assertThat(accessTokenClaims.getExpiration()).isAfter(new Date());
    }

    @DisplayName("리프레시 토큰에 올바른 클레임이 담긴다.")
    @Test
    void refreshTokenContainsValidClaims() {
        //given
        JwtUtil jwtUtil = jwtUtil(Duration.ofMinutes(5), Duration.ofDays(7));
        User user = getUser();

        //when
        String refreshToken = jwtUtil.createRefreshToken(user);

        //then
        Claims refreshTokenClaims = jwtUtil.getClaims(refreshToken);
        assertThat(refreshTokenClaims.getIssuer()).isEqualTo(ISSUER);
        assertThat(refreshTokenClaims.getSubject()).isEqualTo(user.getUserId().toString());
        assertThat(refreshTokenClaims.get("typ", String.class)).isEqualTo("refresh");
        assertThat(refreshTokenClaims.getExpiration()).isAfter(new Date());
    }

    @DisplayName("리프레시 토큰일 경우에만 리프레시 토큰 검증에 성공한다.")
    @Test
    void validateRefreshTokenSuccessfully_WhenRefreshToken() {
        //given
        JwtUtil jwtUtil = jwtUtil(Duration.ofMinutes(5), Duration.ofDays(7));
        User user = getUser();

        String refreshToken = jwtUtil.createRefreshToken(user);
        String accessToken = jwtUtil.createAccessToken(user);

        //when, then
        assertThat(jwtUtil.isValidRefreshToken(refreshToken)).isTrue();
        assertThat(jwtUtil.isValidRefreshToken(accessToken)).isFalse();
    }

    @DisplayName("토큰이 변조되면 파싱, 검증에 실패한다.")
    @Test
    void failToParseOrValidateToken_whenTokenIsTampered() {
        //given
        JwtUtil jwtUtil = jwtUtil(Duration.ofMinutes(5), Duration.ofDays(7));
        String refreshToken = jwtUtil.createRefreshToken(getUser());

        //when
        String tampered = refreshToken+"x";

        //then
        assertThat(jwtUtil.isValidRefreshToken(tampered)).isFalse();
        assertThatThrownBy(()->jwtUtil.getClaims(tampered))
                .isInstanceOf(JwtException.class);
    }

    @DisplayName("만료된 토큰은 검증에 실패한다.")
    @Test
    void failToValidateToken_whenTokenIsExpired() throws InterruptedException {
        //given
        JwtUtil jwtUtil = jwtUtil(Duration.ofSeconds(1), Duration.ofSeconds(1));
        String refreshToken = jwtUtil.createRefreshToken(getUser());

        //when
        Thread.sleep(1200);

        //then
        assertThat(jwtUtil.isValidRefreshToken(refreshToken)).isFalse();
        assertThatThrownBy(()->jwtUtil.getClaims(refreshToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private User getUser() {
        return User.builder()
                .userId(UUID.randomUUID())
                .id("test")
                .password("password1234")
                .username("테스트 유저")
                .email("test@test.com")
                .nickname("테스트트")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
    }

}
