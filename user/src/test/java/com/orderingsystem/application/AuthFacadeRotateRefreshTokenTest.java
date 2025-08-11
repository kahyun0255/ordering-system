package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.CustomerOutboxRepository;
import com.orderingsystem.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeRotateRefreshTokenTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CustomerOutboxRepository customerOutboxRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenTtl;

    private final UUID userId = UUID.randomUUID();
    private final String id = "testId";
    private final String password = "testpassword";
    private String refreshToken;
    private Key key;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        customerOutboxRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        String encodePassword = passwordEncoder.encode(password);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        User user = User.builder()
                .userId(userId)
                .id(id)
                .password(encodePassword)
                .email("test@test.com")
                .nickname("테스트유저")
                .username("테스트유저")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build();

        refreshToken = jwtUtil.createRefreshToken(user);

        userRepository.save(user);

        refreshTokenRepository.save(userId, refreshToken, refreshTokenTtl);
    }

    @DisplayName("refreshToken이 만료되지 않았고, 저장된 값하고 동일하면 AccessToken, RefreshToken 재발급에 성공한다.")
    @Test
    void reissueTokens_whenRefreshTokenIsValidAndMatchesStoredValue() {
        //when
        TokenResponse tokenResponse = authFacade.rotateRefreshAndIssueAccess(refreshToken);

        //then
        assertThat(refreshToken).isNotEqualTo(tokenResponse.getRefreshToken());

        String findRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(findRefreshToken).isEqualTo(tokenResponse.getRefreshToken());
    }

    @DisplayName("토큰 Type이 Refresh가 아닐 경우 토큰 검증 시 예외가 발생한다.")
    @Test
    void failToValidateToken_whenTypeIsNotRefresh() {
        //given
        String token = buildToken("create", issuer, Instant.now().plusSeconds(60));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(token))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 만료되었습니다.");
    }

    @DisplayName("RefreshToken이 만료되면 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenExpired() {
        //given
        String token = buildToken("refresh", issuer, Instant.now().minusSeconds(10));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(token))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 만료되었습니다.");
    }

    @DisplayName("issuer가 일치하지 않으면 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenIssuerMismatch() {
        //given
        String refreshToken = buildToken("refresh", "another-issure", Instant.now().plusSeconds(60));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 만료되었습니다.");
    }

    @DisplayName("서명이 변조된 토큰은 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenTokenTampered() {
        //given
        User user = User.builder()
                .userId(userId)
                .id(id)
                .password(password)
                .email("test@test.com")
                .nickname("테스트유저")
                .username("테스트유저")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build();

        String refreshToken = jwtUtil.createRefreshToken(user);
        String tampered = refreshToken.substring(0, refreshToken.length() - 2) + "aa";

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(tampered))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 만료되었습니다.");
    }

    @DisplayName("저장된 RefreshToken과 다를 경우, 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenTokenIsDifferentFromStored() {
        //given
        User user = User.builder()
                .userId(userId)
                .id(id)
                .password(password)
                .email("test@test.com")
                .nickname("테스트유저")
                .username("테스트유저")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build();

        String refreshToken = jwtUtil.createRefreshToken(user);

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 일치하지 않습니다.");
    }

    private String buildToken(String typ, String iss, Instant exp) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(iss)
                .claim("typ", typ)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }

}
