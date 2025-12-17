package com.orderingsystem.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.UserOutboxRepository;
import com.orderingsystem.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController 리프레시 토큰 통합 테스트")
class AuthControllerRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserOutboxRepository userOutboxRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

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
        userOutboxRepository.deleteAllInBatch();
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

    @DisplayName("RefreshToken 재발급에 성공한다.")
    @Test
    void succeedToReissueRefreshToken() throws Exception {
        //given
        String oldRefreshToken = refreshTokenRepository.findByUserId(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", refreshToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.notNullValue()))
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(json, TokenResponse.class);

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(storedRefreshToken).isNotEqualTo(oldRefreshToken);
        assertThat(storedRefreshToken).isEqualTo(tokenResponse.getRefreshToken());

        String header = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).contains("refreshToken=" + tokenResponse.getRefreshToken());
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("Path=/");
        assertThat(header).contains("SameSite=None");
        assertThat(header).contains("Max-Age=1209600");
    }

    @DisplayName("RefreshToken이 만료되었을 경우, 예외가 발생한다.")
    @Test
    void failToReissueToken_whenRefreshTokenExpired() throws Exception {
        //given
        String token = buildToken("refresh", issuer, Instant.now().minusSeconds(10));

        //when, then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh Token이 만료되었습니다."))
                .andReturn();
    }

    @DisplayName("토큰 Type이 Refresh가 아닐 경우, 예외가 발생한다.")
    @Test
    void failToReissueToken_whenTokenTypeIsNotRefresh() throws Exception {
        //given
        String token = buildToken("create", issuer, Instant.now().plusSeconds(60));

        //when, then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh Token이 만료되었습니다."))
                .andReturn();
    }

    @DisplayName("issuer가 일치하지 않을 경우, 예외가 발생한다.")
    @Test
    void failToReissueToken_whenIssuerDoesNotMatch() throws Exception {
        //given
        String token = buildToken("refresh", "another-issure", Instant.now().plusSeconds(60));

        //when, then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", token))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh Token이 만료되었습니다."))
                .andReturn();
    }

    @DisplayName("토큰의 서명이 변조되었을 경우, 예외가 발생한다.")
    @Test
    void failToReissueToken_whenSignatureIsTampered() throws Exception {
        //given
        String tampered = refreshToken.substring(0, refreshToken.length() - 2) + "aaaa";

        //when, then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", tampered))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh Token이 만료되었습니다."))
                .andReturn();
    }

    @DisplayName("저장된 RefreshToken과 다를 경우, 예외가 발생한다.")
    @Test
    void failToReissueToken_whenStoredRefreshTokenDoesNotMatch() throws Exception {
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
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new Cookie("refreshToken", refreshToken))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh Token이 일치하지 않습니다."))
                .andReturn();
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
