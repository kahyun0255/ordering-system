package com.orderingsystem.application;

import com.orderingsystem.domain.repository.RefreshTokenRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenTtl;

    @Transactional
    public void saveRefreshToken(UUID userId, String refreshToken) {
        refreshTokenRepository.save(userId, refreshToken, refreshTokenTtl);
    }

    @Transactional
    public String findRefreshToken(UUID userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    @Transactional
    public void updateToken(UUID userId, String newRefreshToken) {
        refreshTokenRepository.update(userId, newRefreshToken, refreshTokenTtl);
    }
}
