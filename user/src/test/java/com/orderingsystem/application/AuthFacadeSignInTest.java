package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.application.dto.request.SignInApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.domain.exception.UserNotFoundException;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserStatus;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.UserOutboxRepository;
import com.orderingsystem.util.JwtUtil;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeSignInTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserOutboxRepository userOutboxRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private Duration ttl;

    private final UUID userId = UUID.randomUUID();
    private final String id = "testId";
    private final String password = "testpassword";

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        userOutboxRepository.deleteAllInBatch();
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
                .status(UserStatus.ACTIVE)
                .build();

        String refreshToken = jwtUtil.createRefreshToken(user);

        userRepository.save(user);

        refreshTokenRepository.save(userId, refreshToken, ttl);
    }

    @DisplayName("로그인에 성공한다.")
    @Test
    void signIn() {
        //given
        SignInApplicationRequest signInApplicationRequest = SignInApplicationRequest.builder()
                .id(id)
                .password(password)
                .build();

        String oldRefreshToken = refreshTokenRepository.findByUserId(userId);

        //when
        TokenResponse tokenResponse = authFacade.signIn(signInApplicationRequest);

        //then
        assertThat(tokenResponse.getAccessToken()).isNotBlank();
        assertThat(tokenResponse.getRefreshToken()).isNotBlank();
        assertThat(tokenResponse.getRefreshToken()).isNotEqualTo(oldRefreshToken);

        String storedRefreshToken = refreshTokenRepository.findByUserId(userId);
        assertThat(storedRefreshToken).isEqualTo(tokenResponse.getRefreshToken());
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToLogin_whenPasswordIsIncorrect() {
        //given
        SignInApplicationRequest signInApplicationRequest = SignInApplicationRequest.builder()
                .id(id)
                .password("password1234")
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(signInApplicationRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @DisplayName("아이디가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToLogin_whenIdIsIncorrect() {
        //given
        SignInApplicationRequest signInApplicationRequest = SignInApplicationRequest.builder()
                .id("ididid")
                .password(password)
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(signInApplicationRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @DisplayName("ACTIVE 상태가 아닌 사용자는 로그인에 실패한다.")
    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = "ACTIVE", mode = Mode.EXCLUDE)
    void failToSignIn_whenUserIsNotActive(UserStatus status) {
        //given
        UUID inactiveUserId = UUID.randomUUID();
        userRepository.save(createUser(inactiveUserId, "inactiveId", status));

        SignInApplicationRequest request = SignInApplicationRequest.builder()
                .id("inactiveId")
                .password(password)
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("탈퇴했거나 사용할 수 없는 계정입니다.");

        assertThat(refreshTokenRepository.findByUserId(inactiveUserId)).isNull();
    }

    @DisplayName("탈퇴한 사용자가 틀린 비밀번호로 로그인하면 비밀번호 오류가 먼저 발생한다.")
    @Test
    void failToSignIn_whenWithdrawnUserPasswordIsIncorrect() {
        //given
        UUID withdrawnUserId = UUID.randomUUID();
        userRepository.save(createUser(withdrawnUserId, "withdrawnId", UserStatus.WITHDRAWN));

        SignInApplicationRequest signInApplicationRequest = SignInApplicationRequest.builder()
                .id("withdrawnId")
                .password("wrongpassword")
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(signInApplicationRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    private User createUser(UUID userId, String id, UserStatus status) {
        return User.builder()
                .userId(userId)
                .id(id)
                .password(passwordEncoder.encode(password))
                .email(id + "@test.com")
                .nickname(id)
                .username("비활성유저")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .status(status)
                .build();
    }

}
