package com.orderingsystem.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.domain.repository.outbox.UserOutboxRepository;
import com.orderingsystem.presentation.request.SignUpRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController 회원가입 통합 테스트")
class UserControllerSignUpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOutboxRepository userOutboxRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        userOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void signUp() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        //when
        String json = mockMvc.perform(
                        post("/api/auth/sign-up")
                                .content(objectMapper.writeValueAsString(signUpRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        //then
        Optional<User> user = userRepository.findById(signUpRequest.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo(signUpRequest.getEmail());
        assertThat(user.get().getType()).isEqualTo(signUpRequest.getType());

        assertThat(userOutboxRepository.count()).isEqualTo(1L);
    }

    @DisplayName("RequestBody 검증에 실패할 경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenRequestBodyValidationFails() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("")
                .id("t")
                .password("passw")
                .email("test")
                .nickname("테스트유저")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .content(objectMapper.writeValueAsString(signUpRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("password: 비밀번호는 8 ~ 64자여야 합니다."),
                        containsString("username: 이름은 생략이 불가능합니다."),
                        containsString("email: 유효한 이메일 형식이 아닙니다."),
                        containsString("id: 아이디는 2 ~ 30자여야 합니다.")
                )));
    }

    @DisplayName("아이디가 중복될경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenIdIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id(getSignUpRequest().getId())
                .password("password234")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .username("테스트유저2")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .content(objectMapper.writeValueAsString(signUpRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다."));
    }

    @DisplayName("닉네임이 중복될경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenNicknameIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id("testId2")
                .password("password234")
                .email("test2@test.com")
                .nickname(signUpRequest.getNickname())
                .username("테스트유저2")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .content(objectMapper.writeValueAsString(signUpRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."));
    }

    @DisplayName("이메일이 중복될경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenEmailIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        userRepository.save(User.builder()
                .userId(UUID.randomUUID())
                .id("testId2")
                .password("password234")
                .email(signUpRequest.getEmail())
                .nickname("테스트유저2")
                .username("테스트유저2")
                .type(UserType.CUSTOMER)
                .phoneNumber("010-1234-5678")
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .content(objectMapper.writeValueAsString(signUpRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다."));
    }

    private SignUpRequest getSignUpRequest() {
        return SignUpRequest.builder()
                .username("테스트 유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트유저")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
    }

}
