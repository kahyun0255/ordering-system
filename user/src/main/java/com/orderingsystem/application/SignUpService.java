package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.application.outbox.UserOutboxPolicyRegistry;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.RefreshToken;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignUpService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserServiceHelper userServiceHelper;
    private final UserOutboxPolicyRegistry userOutboxPolicyRegistry;

    @Transactional
    public TokenResponse signUp(SignUpApplicationRequest signUpApplicationRequest) {
        log.info("회원가입을 진행합니다. userId : {}", signUpApplicationRequest.getId());

        UserCreatedEvent userCreatedEvent = userServiceHelper.persistUser(signUpApplicationRequest);

        String accessToken = jwtUtil.createAccessToken(userCreatedEvent.getUser());
        String refreshToken = jwtUtil.createRefreshToken(userCreatedEvent.getUser());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userCreatedEvent.getUser().getUserId())
                .token(refreshToken)
                .build());

        userOutboxPolicyRegistry.get(signUpApplicationRequest.getType())
                .ifPresentOrElse(p -> p.saveOutbox(userCreatedEvent),
                        ()->log.warn("outbox policy가 없습니다. {}", signUpApplicationRequest.getType()));

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}
