package com.orderingsystem.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.presentation.request.UpdateUserRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController 통합 테스트")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @DisplayName("로그인한 유저의 프로필 정보를 조회한다.")
    @Test
    void shouldRetrieveLoggedInUserProfile() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getUserId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.type").value(user.getType().name()))
                .andReturn();
    }

    @DisplayName("accessToken에 해당하는 사용자 정보가 없으면 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenAccessTokenUserDoesNotExist() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."))
                .andReturn();
    }

    @DisplayName("accessToken이 만료되었으면 프로필 정보 조회에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenAccessTokenExpired() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getUserId(), "access", issuer, Instant.now().minusSeconds(1));

        //when, then
        mockMvc.perform(
                        get("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("AccessToken 검증에 실패했습니다."))
                .andReturn();
    }

    @DisplayName("로그인한 사용자의 정보 업데이트에 성공한다.")
    @Test
    void shouldUpdateLoggedInUserInfoSuccessfully() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getUserId(), "access", issuer, Instant.now().plusSeconds(100000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.phoneNumber").value(user.getPhoneNumber()))
                .andExpect(jsonPath("$.type").value(user.getType().name()))
                .andReturn();
    }

    @DisplayName("accessToken에 해당하는 사용자 정보가 없을 경우 정보 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateUserInfo_whenAccessTokenUserDoesNotExist() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."))
                .andReturn();
    }

    @DisplayName("회원 탈퇴에 성공하면 User의 status가 WITHDRAWN이 된다.")
    @Test
    void shouldUpdateUserStatusToWithdrawn_whenUserSuccessfullyWithdraws() throws Exception {
        //given
        User user = User.builder()
                .userId(UUID.randomUUID())
                .id("testID")
                .password("testPassword")
                .email("test@test.com")
                .username("테스트 유저")
                .nickname("테스트 유저 닉네임")
                .phoneNumber("010-1234-5678")
                .type(UserType.CUSTOMER)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getUserId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        delete("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @DisplayName("accessToken에 해당하는 사용자가 없으면 회원 탈퇴에 실패하고 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenAccessTokenUserDoesNotExistOnWithdrawal() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        delete("/api/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."))
                .andReturn();
    }

    private String buildToken(UUID userId, String typ, String iss, Instant exp) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(iss)
                .claim("userId", userId.toString())
                .claim("typ", typ)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

}
