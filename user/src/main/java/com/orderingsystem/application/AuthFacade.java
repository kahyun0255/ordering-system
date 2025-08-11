package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignInApplicationRequest;
import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthFacade {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CommonJwtUtil commonJwtUtil;
    private final RefreshTokenService refreshTokenService;

    public TokenResponse signUp(SignUpApplicationRequest signUpApplicationRequest) {
        log.info("회원가입을 진행합니다. id : {}", signUpApplicationRequest.getId());

        UserCreatedEvent userCreatedEvent = userService.persistUser(signUpApplicationRequest);

        String accessToken = jwtUtil.createAccessToken(userCreatedEvent.getUser());
        String refreshToken = jwtUtil.createRefreshToken(userCreatedEvent.getUser());

        refreshTokenService.saveRefreshToken(userCreatedEvent.getUser().getUserId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse signIn(SignInApplicationRequest signInApplicationRequest) {
        User user = userService.findUser(signInApplicationRequest.getId());
        userService.verifyPassword(signInApplicationRequest.getPassword(), user.getPassword());

        String newRefreshToken = jwtUtil.createRefreshToken(user);
        refreshTokenService.updateToken(user.getUserId(), newRefreshToken);

        String newAccessToken = jwtUtil.createAccessToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public TokenResponse rotateRefreshAndIssueAccess(String refreshToken) {
        try {
            if (!commonJwtUtil.isValidRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException("Refresh Token이 만료되었습니다.");
            }

            Claims claims = jwtUtil.getClaims(refreshToken);
            String userId = claims.getSubject();
            User user = userService.findUserByUserId(UUID.fromString(userId));

            String findRefreshToken = refreshTokenService.findRefreshToken(user.getUserId());
            if (!refreshToken.equals(findRefreshToken)) {
                throw new InvalidCredentialsException("Refresh Token이 일치하지 않습니다.");
            }

            String newRefreshToken = jwtUtil.createRefreshToken(user);
            refreshTokenService.updateToken(user.getUserId(), newRefreshToken);

            String newAccessToken = jwtUtil.createAccessToken(user);

            return new TokenResponse(newAccessToken, newRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidCredentialsException("Refresh Token 파싱/검증에 실패했습니다.");
        }
    }

}
