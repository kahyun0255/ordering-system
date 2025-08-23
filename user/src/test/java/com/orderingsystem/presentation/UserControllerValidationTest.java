package com.orderingsystem.presentation;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.AuthFacade;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.presentation.request.SignInRequest;
import com.orderingsystem.presentation.request.SignUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RequestBody 검증 테스트")
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthFacade authFacade;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private CommonJwtUtil commonJwtUtil;

    @DisplayName("회원가입에 성공한다.")
    @Test
    void signUp() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("test")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(status().isOk());
    }

    @DisplayName("회원가입 시, 사용자 이름은 필수값이다.")
    @Test
    void failToSignUp_whenUsernameIsMissing() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .id("test")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("username: 이름은 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 사용자 이름은 비어있으면 안된다.")
    @Test
    void failToSignUp_whenUsernameIsBlank() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("")
                .id("test")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("username: 이름은 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 아아디는 필수값이다.")
    @Test
    void failToSignUp_whenIdIsMissing() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 아이디는 비어있으면 안된다.")
    @Test
    void failToSignUp_whenIdIsBlank() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("")
                .password("testpassword")
                .email("test@test.com")
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
                        containsString("id: 아이디는 생략이 불가능합니다."),
                        containsString("id: 아이디는 2 ~ 30자여야 합니다.")
                )));
    }

    @DisplayName("회원가입 시, 아이디는 두 글자보다 길어야 한다.")
    @Test
    void failToSignUp_whenIdIsTooShort() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("i")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("id: 아이디는 2 ~ 30자여야 합니다."));
    }

    @DisplayName("회원가입 시, 아이디는 30자보다 짧아야 한다.")
    @Test
    void failToSignUp_whenIdExceedsMaxLength() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("0123456789012345678901234567890")
                .password("testpassword")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("id: 아이디는 2 ~ 30자여야 합니다."));
    }

    @DisplayName("회원가입 시, 비밀번호는 필수값이다.")
    @Test
    void failToSignUp_whenPasswordIsMissing() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 비밀번호는 비어있으면 안된다.")
    @Test
    void failToSignUp_whenPasswordIsBlank() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("")
                .email("test@test.com")
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
                        containsString("password: 비밀번호는 생략이 불가능합니다."),
                        containsString("password: 비밀번호는 8 ~ 64자여야 합니다.")
                )));
    }

    @DisplayName("회원가입 시, 비밀번호는 8자보다 길어야 한다.")
    @Test
    void failToSignUp_whenPasswordIsTooShort() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("1234567")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 8 ~ 64자여야 합니다."));
    }

    @DisplayName("회원가입 시, 비밀번호는 64자보다 짧아야 한다.")
    @Test
    void failToSignUp_whenPasswordExceedsMaxLength() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("0123456789012345678901234567890012345678901234567890123456789012345")
                .email("test@test.com")
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
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 8 ~ 64자여야 합니다."));
    }

    @DisplayName("회원가입 시, 이메일은 필수값이다.")
    @Test
    void failToSignUp_whenEmailIsMissing() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
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
                .andExpect(jsonPath("$.message").value("email: 이메일은 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 이메일은 비어있으면 안된다.")
    @Test
    void failToSignUp_whenEmailIsBlank() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("")
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
                .andExpect(jsonPath("$.message").value("email: 이메일은 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 이메일 형식이 올바르지 않으면 안된다.")
    @Test
    void failToSignUp_whenEmailFormatIsInvalid() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
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
                .andExpect(jsonPath("$.message").value("email: 유효한 이메일 형식이 아닙니다."));
    }

    @DisplayName("회원가입 시, 닉네임은 필수값이다.")
    @Test
    void failToSignUp_whenNicknameIsMissing() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .email("test@test.com")
                .password("password")
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
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 생략이 불가능합니다."));
    }

    @DisplayName("회원가입 시, 닉네임은 비어있으면 안된다.")
    @Test
    void failToSignUp_whenNicknameIsBlank() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("")
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
                        containsString("nickname: 닉네임은 생략이 불가능합니다."),
                        containsString("nickname: 닉네임은 2 ~ 30자여야 합니다.")
                )));
    }

    @DisplayName("회원가입 시, 닉네임은 두 글자보다 길어야 한다.")
    @Test
    void failToSignUp_whenNicknameIsTooShort() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("닉")
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
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));
    }

    @DisplayName("회원가입 시, 닉네임은 30자보다 짧아야 한다.")
    @Test
    void failToSignUp_whenNicknameExceedsMaxLength() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("1234567890123456789012345678901")
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
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));
    }

    @DisplayName("회원가입 시, 휴대폰 번호 형식이 올바르지 않으면 안된다.")
    @Test
    void failToSignUp_whenPhoneNumberFormatIsInvalid() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트유저")
                .phoneNumber("010-12-3456")
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
                .andExpect(jsonPath("$.message").value("phoneNumber: 유효한 휴대폰 번호 형식이 아닙니다."));
    }

    @DisplayName("회원가입 시, 휴대폰 번호 형식이 올바르지 않으면 안된다.")
    @Test
    void failToSignUp_whenPhoneNumberFormatIsInvalid2() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트유저")
                .phoneNumber("010-12345-3456")
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
                .andExpect(jsonPath("$.message").value("phoneNumber: 유효한 휴대폰 번호 형식이 아닙니다."));
    }

    @DisplayName("회원가입 시, 휴대폰 번호 형식이 올바르지 않으면 안된다.")
    @Test
    void failToSignUp_whenPhoneNumberFormatIsInvalid3() throws Exception {
        //given
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username("테스트유저")
                .id("testId")
                .password("password1234")
                .email("test@test.com")
                .nickname("테스트유저")
                .phoneNumber("123-1234-3456")
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
                .andExpect(jsonPath("$.message").value("phoneNumber: 유효한 휴대폰 번호 형식이 아닙니다."));
    }

    @DisplayName("로그인 시, 아이디는 필수 값이다.")
    @Test
    void failToSignIn_whenIdIsMissing() throws Exception {
        //given
        SignInRequest signInRequest = SignInRequest.builder()
                .password("password")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));
    }

    @DisplayName("로그인 시, 아이디는 비어있으면 안 된다.")
    @Test
    void failToSignIn_whenIdIsBlank() throws Exception {
        //given
        SignInRequest signInRequest = SignInRequest.builder()
                .id("")
                .password("password1234")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));
    }

    @DisplayName("로그인 시, 비밀번호는 필수 값이다.")
    @Test
    void failToSignIn_whenPasswordIsMissing() throws Exception {
        //given
        SignInRequest signInRequest = SignInRequest.builder()
                .id("iddddd")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));
    }

    @DisplayName("로그인 시, 비밀번호는 비어있으면 안 된다.")
    @Test
    void failToSignIn_whenPasswordIsBlank() throws Exception {
        //given
        SignInRequest signInRequest = SignInRequest.builder()
                .id("iddddd")
                .password("")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .content(objectMapper.writeValueAsString(signInRequest))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));
    }

}
