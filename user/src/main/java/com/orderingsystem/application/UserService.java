package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignInApplicationRequest;
import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.application.outbox.UserOutboxPolicyRegistry;
import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.exception.UserNotFoundException;
import com.orderingsystem.domain.model.RefreshToken;
import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.domain.repository.UserRepository;
import com.orderingsystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserServiceHelper userServiceHelper;
    private final UserOutboxPolicyRegistry userOutboxPolicyRegistry;
    private final UserRepository userRepository;
    private final CommonJwtUtil commonJwtUtil;

    @Transactional
    public TokenResponse signUp(SignUpApplicationRequest signUpApplicationRequest) {
        log.info("회원가입을 진행합니다. id : {}", signUpApplicationRequest.getId());

        UserCreatedEvent userCreatedEvent = userServiceHelper.persistUser(signUpApplicationRequest);

        String accessToken = jwtUtil.createAccessToken(userCreatedEvent.getUser());
        String refreshToken = jwtUtil.createRefreshToken(userCreatedEvent.getUser());

        saveRefreshToken(userCreatedEvent, refreshToken);

        userOutboxPolicyRegistry.get(signUpApplicationRequest.getType())
                .ifPresentOrElse(p -> p.saveOutbox(userCreatedEvent),
                        () -> log.warn("outbox policy가 없습니다. {}", signUpApplicationRequest.getType()));

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse signIn(SignInApplicationRequest signInApplicationRequest) {
        User user = findUser(signInApplicationRequest.getId());
        userServiceHelper.verifyPassword(signInApplicationRequest.getPassword(), user.getPassword());

        RefreshToken refreshToken = findRefreshToken(user);
        String newRefreshToken = jwtUtil.createRefreshToken(user);
        refreshToken.updateToken(newRefreshToken);

        String newAccessToken = jwtUtil.createAccessToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public TokenResponse rotateRefreshAndIssueAccess(String refreshToken) {
        try {
            if (!commonJwtUtil.isValidRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException("Refresh Token이 만료되었습니다.");
            }

            Claims claims = jwtUtil.getClaims(refreshToken);
            String userId = claims.getSubject();
            User user = findUserByUserId(UUID.fromString(userId));

            RefreshToken findRefreshToken = findRefreshToken(user);
            if (!refreshToken.equals(findRefreshToken.getToken())) {
                throw new InvalidCredentialsException("Refresh Token이 일치하지 않습니다.");
            }

            String newRefreshToken = jwtUtil.createRefreshToken(user);
            findRefreshToken.updateToken(newRefreshToken);

            String newAccessToken = jwtUtil.createAccessToken(user);

            return new TokenResponse(newAccessToken, newRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidCredentialsException("Refresh Token 파싱/검증에 실패했습니다.");
        }
    }

    private void saveRefreshToken(UserCreatedEvent userCreatedEvent, String refreshToken) {
        try {
            refreshTokenRepository.save(RefreshToken.builder()
                    .userId(userCreatedEvent.getUser().getUserId())
                    .token(refreshToken)
                    .build());
        } catch (DataIntegrityViolationException e) {
            refreshTokenRepository.findByUserId(userCreatedEvent.getUser().getUserId())
                    .ifPresent(rt -> rt.updateToken(refreshToken));
        }
    }

    private User findUser(String id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자입니다. Id : {}", id);
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return user.get();
    }

    private User findUserByUserId(UUID userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            log.warn("존재하지 않는 사용자입니다. User Id : {}", userId);
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return user.get();
    }

    private RefreshToken findRefreshToken(User user) {
        return refreshTokenRepository.findByUserId(user.getUserId())
                .orElseGet(() -> refreshTokenRepository.save(RefreshToken.builder()
                        .userId(user.getUserId())
                        .token(jwtUtil.createRefreshToken(user))
                        .build()));
    }

}
