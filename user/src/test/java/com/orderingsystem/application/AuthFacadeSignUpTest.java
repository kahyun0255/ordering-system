package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.CustomerOutboxRepository;
import com.orderingsystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeSignUpTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerOutboxRepository customerOutboxRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.issuer}")
    private String issuer;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        customerOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void signUp() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();

        //when
        TokenResponse tokenResponse = authFacade.signUp(signUpApplicationRequest);

        //then
        Optional<User> user = userRepository.findById(signUpApplicationRequest.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo(signUpApplicationRequest.getEmail());
        assertThat(user.get().getType()).isEqualTo(signUpApplicationRequest.getType());

        assertThat(tokenResponse.getAccessToken()).isNotBlank();
        assertThat(tokenResponse.getRefreshToken()).isNotBlank();
    }

    @DisplayName("비밀번호가 암호화되어 저장된다.")
    @Test
    void encryptPasswordBeforeSaving() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();

        //when
        authFacade.signUp(signUpApplicationRequest);

        //then
        Optional<User> user = userRepository.findById(signUpApplicationRequest.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getPassword()).isNotEqualTo(signUpApplicationRequest.getPassword());
        assertThat(new BCryptPasswordEncoder().matches(signUpApplicationRequest.getPassword(),
                user.get().getPassword())).isTrue();
    }

    @DisplayName("회원가입 후 발급된 토큰이 올바른 정보를 담고있으며, 유효하다.")
    @Test
    void tokenContainsValidInformation() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();

        //when
        TokenResponse tokenResponse = authFacade.signUp(signUpApplicationRequest);

        //then
        Optional<User> user = userRepository.findById(signUpApplicationRequest.getId());
        assertThat(user).isPresent();

        Claims accessTokenClaims = jwtUtil.getClaims(tokenResponse.getAccessToken());
        assertThat(accessTokenClaims.getIssuer()).isEqualTo(issuer);
        assertThat(accessTokenClaims.get("typ", String.class)).isEqualTo("access");
        assertThat(accessTokenClaims.get("userId", String.class)).isEqualTo(user.get().getUserId().toString());
        assertThat(accessTokenClaims.getExpiration()).isAfter(new Date());
        assertThat(jwtUtil.isValidRefreshToken(tokenResponse.getAccessToken())).isFalse();

        Claims refreshTokenClaims = jwtUtil.getClaims(tokenResponse.getRefreshToken());
        assertThat(refreshTokenClaims.getIssuer()).isEqualTo(issuer);
        assertThat(refreshTokenClaims.get("typ", String.class)).isEqualTo("refresh");
        assertThat(refreshTokenClaims.getExpiration()).isAfter(new Date());
        assertThat(jwtUtil.isValidRefreshToken(tokenResponse.getRefreshToken())).isTrue();
    }

    @DisplayName("UserType이 Customer일 때, CustomerOutbox에 저장된다.")
    @Test
    void saveToCustomerOutbox_whenUserTypeIsCustomer() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();

        //when
        authFacade.signUp(signUpApplicationRequest);

        //then
        assertThat(customerOutboxRepository.count()).isEqualTo(1L);
    }

    @DisplayName("UserType이 Customer가 아닐 경우, CustomerOutbox에 저장되지 않는다.")
    @Test
    void doNotSaveToCustomerOutbox_whenUserTypeIsNotCustomer() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = SignUpApplicationRequest.builder()
                .username("테스트 유저")
                .id("test")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트트")
                .phoneNumber("010-1234-5678")
                .type(UserType.RESTAURANT_OWNER)
                .build();
        ;

        //when
        authFacade.signUp(signUpApplicationRequest);

        //then
        assertThat(customerOutboxRepository.count()).isEqualTo(0L);
    }

    @DisplayName("아이디가 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenIdIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id(signUpApplicationRequest.getId())
                .username("username")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1111-2222")
                .nickname("nickname2")
                .email("email2@eamil.com")
                .password("password2")
                .build());

        //when, then
        assertThatThrownBy(() -> authFacade.signUp(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @DisplayName("이메일이 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenEmailIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id("testId2")
                .username("username")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1111-2222")
                .nickname("nickname2")
                .email(signUpApplicationRequest.getEmail())
                .password("password2")
                .build());

        //when, then
        assertThatThrownBy(() -> authFacade.signUp(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 이메일입니다.");
    }

    @DisplayName("닉네임이 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenNicknameIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id("testId2")
                .username("username")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1111-2222")
                .nickname(signUpApplicationRequest.getNickname())
                .email("test2@test.com")
                .password("password2")
                .build());

        //when, then
        assertThatThrownBy(() -> authFacade.signUp(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");
    }

    private SignUpApplicationRequest getSignUpApplicationRequest() {
        return SignUpApplicationRequest.builder()
                .username("테스트 유저")
                .id("test")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트트")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
    }

}
