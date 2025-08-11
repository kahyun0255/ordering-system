package com.orderingsystem.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.AuthFacade;
import com.orderingsystem.application.UserService;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.domain.exception.UserNotFoundException;
import com.orderingsystem.presentation.request.SignInRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController 로그인 단위테스트")
class UserControllerSignInWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthFacade authFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private UserService userService;

    @DisplayName("로그인에 성공한다.")
    @Test
    void signIn_success() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest("id", "password");

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.jwt.token")
                .build();

        given(authFacade.signIn(any())).willReturn(tokenResponse);

        //when, then
        mockMvc.perform(post("/api/auth/sign-in")
                        .content(objectMapper.writeValueAsString(signInRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt.token"));
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToSignIn_whenPasswordDoesNotMatch() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest("id", "password");

        given(authFacade.signIn(any()))
                .willThrow(new InvalidCredentialsException("비밀번호가 일치하지 않습니다."));

        // when, then
        mockMvc.perform(post("/api/auth/sign-in")
                        .content(objectMapper.writeValueAsString(signInRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
                .andExpect(header().doesNotExist("Set-Cookie")); // 쿠키 세팅 안됨 확인
    }

    @DisplayName("아이디가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToSignIn_whenIdDoesNotMatch() throws Exception {
        //given
        SignInRequest signInRequest = getSignInRequest("id", "password");

        given(authFacade.signIn(any()))
                .willThrow(new UserNotFoundException("존재하지 않는 사용자입니다."));

        // when, then
        mockMvc.perform(post("/api/auth/sign-in")
                        .content(objectMapper.writeValueAsString(signInRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."))
                .andExpect(header().doesNotExist("Set-Cookie")); // 쿠키 세팅 안됨 확인
    }

    private SignInRequest getSignInRequest(String id, String password) {
        return SignInRequest.builder()
                .id(id)
                .password(password)
                .build();
    }

}
