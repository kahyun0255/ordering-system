package com.orderingsystem.application;

import com.orderingsystem.application.dto.request.SignUpApplicationRequest;
import com.orderingsystem.application.dto.response.TokenResponse;
import com.orderingsystem.application.mapper.UserDataMapper;
import com.orderingsystem.application.outbox.customer.CustomerOutboxHelper;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.RefreshToken;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.domain.repository.RefreshTokenRepository;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.util.JwtUtil;
import java.util.UUID;
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
    private final UserDataMapper userDataMapper;
    private final UserServiceHelper userServiceHelper;
    private final CustomerOutboxHelper customerOutboxHelper;

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

        saveOutbox(signUpApplicationRequest, userCreatedEvent);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void saveOutbox(SignUpApplicationRequest signUpApplicationRequest, UserCreatedEvent userCreatedEvent) {
        if (signUpApplicationRequest.getType().equals(UserType.CUSTOMER)) {
            customerOutboxHelper.saveCustomerOutboxMessage(
                    userDataMapper.userCreatedToUserCreateEventPayload(userCreatedEvent),
                    OutboxStatus.STARTED,
                    UUID.randomUUID());
        }
    }

}
