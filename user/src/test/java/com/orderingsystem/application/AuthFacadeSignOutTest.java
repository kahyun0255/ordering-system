package com.orderingsystem.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AuthFacade 로그아웃 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthFacadeSignOutTest {

    @Mock
    private CommonJwtUtil commonJwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthFacade authFacade;

    private final UUID userId = UUID.randomUUID();
    private final String refreshToken = "refreshToken";

    @DisplayName("로그아웃에 성공한다.")
    @Test
    void signOut_shouldRevoke_whenEverythingValid() {
        //given
        given(commonJwtUtil.isValidRefreshToken(refreshToken)).willReturn(true);

        Claims claims = mock(Claims.class);
        given(commonJwtUtil.getClaims(refreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn(userId.toString());
        given(refreshTokenService.findRefreshToken(userId)).willReturn(refreshToken);

        //when, then
        assertDoesNotThrow(() -> authFacade.signOut(userId, refreshToken));
        verify(refreshTokenService).revoke(userId);
    }

    @DisplayName("Refresh Token이 유효하지 않으면 로그아웃에 실패하고, 예외가 발생한다.")
    @Test
    void signOut_shouldThrow_whenRefreshTokenInvalid() {
        //given
        given(commonJwtUtil.isValidRefreshToken(refreshToken)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> authFacade.signOut(userId, refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh Token이 유효하지 않습니다.");

        verifyNoInteractions(refreshTokenService);
    }

    @DisplayName("Subject가 일치하지 않으면 로그아웃에 실패하고, 예외가 발생한다.")
    @Test
    void signOut_shouldThrow_whenTokenSubjectNotMatchUser() {
        //given
        given(commonJwtUtil.isValidRefreshToken(refreshToken)).willReturn(true);
        Claims claims = mock(Claims.class);
        given(commonJwtUtil.getClaims(refreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn(UUID.randomUUID().toString());

        //when, then
        assertThatThrownBy(() -> authFacade.signOut(userId, refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("올바르지 않은 Refresh Token입니다.");

        verifyNoInteractions(refreshTokenService);
    }

    @DisplayName("이미 로그아웃이 되어있으면(저장된 Refresh Token이 없으면) 로그아웃에 실패하고, 예외가 발생한다.")
    @Test
    void signOut_shouldReturnSilently_whenStoredRefreshTokenNotExists() {
        //given
        given(commonJwtUtil.isValidRefreshToken(refreshToken)).willReturn(true);
        Claims claims = mock(Claims.class);
        given(commonJwtUtil.getClaims(refreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn(userId.toString());
        given(refreshTokenService.findRefreshToken(userId)).willReturn(null);

        //when, then
        assertDoesNotThrow(() -> authFacade.signOut(userId, refreshToken));

        verify(refreshTokenService, never()).revoke(any());
    }

    @DisplayName("세션 정보가 일치하지 않으면 로그아웃에 실패하고, 예외가 발생한다.")
    @Test
    void signOut_shouldThrow_whenStoredRefreshTokenDifferent() {
        //given
        given(commonJwtUtil.isValidRefreshToken(refreshToken)).willReturn(true);
        Claims claims = mock(Claims.class);
        given(commonJwtUtil.getClaims(refreshToken)).willReturn(claims);
        given(claims.getSubject()).willReturn(userId.toString());
        given(refreshTokenService.findRefreshToken(userId)).willReturn("other-token");

        //when, then
        assertThatThrownBy(() -> authFacade.signOut(userId, refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("세션 정보가 일치하지 않습니다.");

        verify(refreshTokenService, never()).revoke(any());
    }

}
