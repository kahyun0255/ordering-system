package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.outbox.UserOutboxPolicyRegistry;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthFacadeHelperTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private UserOutboxPolicyRegistry userOutboxPolicyRegistry;

    @DisplayName("user 정보가 성공적으로 저장된다.")
    @Test
    void persistUser() {
        //given
        String encodedPassword = "ENCODED_PASSWORD";

        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        given(userRepository.existsById(signUpApplicationRequest.getId())).willReturn(false);
        given(userRepository.existsByEmail(signUpApplicationRequest.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(signUpApplicationRequest.getNickname())).willReturn(false);
        given(bCryptPasswordEncoder.encode(signUpApplicationRequest.getPassword())).willReturn(encodedPassword);

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        //when
        UserCreatedEvent userCreatedEvent = userService.persistUser(signUpApplicationRequest);

        //then
        then(userRepository).should().save(userArgumentCaptor.capture());
        User savedUser = userArgumentCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(signUpApplicationRequest.getId());
        assertThat(savedUser.getType()).isEqualTo(signUpApplicationRequest.getType());
        assertThat(savedUser.getNickname()).isEqualTo(signUpApplicationRequest.getNickname());
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        then(bCryptPasswordEncoder).should().encode(signUpApplicationRequest.getPassword());
    }

    @DisplayName("아이디가 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenIdIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        given(userRepository.existsById(signUpApplicationRequest.getId())).willReturn(true);

        //when, then
        assertThatThrownBy(() -> userService.persistUser(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 아이디입니다.");

        then(userRepository).should(never()).save(any());
        then(bCryptPasswordEncoder).shouldHaveNoInteractions();
    }

    @DisplayName("닉네임이 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenNicknameIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        given(userRepository.existsByNickname(signUpApplicationRequest.getNickname())).willReturn(true);

        //when, then
        assertThatThrownBy(() -> userService.persistUser(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");

        then(userRepository).should(never()).save(any());
        then(bCryptPasswordEncoder).shouldHaveNoInteractions();
    }

    @DisplayName("이메일이 중복되면 예외가 발생한다.")
    @Test
    void throwException_whenEmailIsDuplicate() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = getSignUpApplicationRequest();
        given(userRepository.existsByEmail(signUpApplicationRequest.getEmail())).willReturn(true);

        //when, then
        assertThatThrownBy(() -> userService.persistUser(signUpApplicationRequest))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        then(userRepository).should(never()).save(any());
        then(bCryptPasswordEncoder).shouldHaveNoInteractions();
    }

    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    @Test
    void verifyPasswordMatchesStoredPassword() {
        //when, then
        assertThatThrownBy(() -> userService.verifyPassword("testpassword1234", "testpassword12345"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
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
