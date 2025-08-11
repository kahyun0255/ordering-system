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
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.CustomerOutboxRepository;
import com.orderingsystem.presentation.request.SignInRequest;
import com.orderingsystem.util.JwtUtil;
import java.time.Duration;
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
@DisplayName("UserController 로그인 통합 테스트")
class UserControllerSignInTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CustomerOutboxRepository customerOutboxRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenTtl;

    private final UUID userId = UUID.randomUUID();
    private final String id = "testId";
    private final String password = "testpassword";

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        customerOutboxRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        String encodePassword = passwordEncoder.encode(password);

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

        String refreshToken = jwtUtil.createRefreshToken(user);

        userRepository.save(user);

        refreshTokenRepository.save(userId, refreshToken, refreshTokenTtl);
    }

    @DisplayName("로그인에 성공한다.")
    @Test
    void signIn() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest(id, password);
        String oldRefreshToken = refreshTokenRepository.findByUserId(userId);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
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

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToSignIn_whenPasswordDoesNotMatch() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest(id, "passssworddddd");
        String oldRefreshToken = refreshTokenRepository.findByUserId(userId);

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
                .andReturn().getResponse().getContentAsString();

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(storedRefreshToken).isEqualTo(oldRefreshToken);
    }

    @DisplayName("아이디가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToSignIn_whenIdDoesNotMatch() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest("iddddddd", password);
        String oldRefreshToken = refreshTokenRepository.findByUserId(userId);

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(storedRefreshToken).isEqualTo(oldRefreshToken);
    }

    @DisplayName("저장된 RefreshToken이 없으면 새로 발급한다.")
    @Test
    void issueNewRefreshToken_whenTokenDoesNotExist() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String id = "iddddd";
        String password = "passsworddd";
        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .userId(userId)
                .id(id)
                .password(encodedPassword)
                .email("test2@test.com")
                .nickname("테스트유저2")
                .username("테스트유저2")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build();

        userRepository.save(user);

        SignInRequest signInRequest = getSignInRequest(id, password);

        //when
        String json = mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        //then
        TokenResponse tokenResponse = objectMapper.readValue(json, TokenResponse.class);

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(storedRefreshToken).isEqualTo(tokenResponse.getRefreshToken());
    }

    private SignInRequest getSignInRequest(String id, String password) {
        return SignInRequest.builder()
                .id(id)
                .password(password)
                .build();
    }

}
