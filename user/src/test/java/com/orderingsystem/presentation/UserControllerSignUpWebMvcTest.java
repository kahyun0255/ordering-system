package com.orderingsystem.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.AuthFacade;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.presentation.request.SignUpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 회원가입 단위테스트")
class UserControllerSignUpWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthFacade authFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @DisplayName("회원가입에 성공한다.")
    @Test
    void signUp_success() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.jwt.token")
                .build();

        given(authFacade.signUp(any())).willReturn(tokenResponse);

        //when, then
        mockMvc.perform(post("/api/auth/sign-up")
                        .content(objectMapper.writeValueAsString(signUpRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt.token"));
    }

    @DisplayName("아이디가 중복 될 경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenIdIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        doThrow(new DuplicateKeyException("이미 존재하는 아이디입니다."))
                .when(authFacade).signUp(any());

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

    @DisplayName("닉네임이 중복 될 경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenNicknameIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        doThrow(new DuplicateKeyException("이미 존재하는 닉네임입니다."))
                .when(authFacade).signUp(any());

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

    @DisplayName("이메일이 중복 될 경우, 회원가입에 실패한다.")
    @Test
    void failToSignUp_whenEmailIsDuplicate() throws Exception {
        //given
        SignUpRequest signUpRequest = getSignUpRequest();

        doThrow(new DuplicateKeyException("이미 존재하는 이메일입니다."))
                .when(authFacade).signUp(any());

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
                .username("테스트유저")
                .id("testId")
                .password("testpassword")
                .email("test@test.com")
                .nickname("테스트유저")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
    }

}
