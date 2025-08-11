package com.orderingsystem.domain.repository;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(UUID userId, String refreshToken, Duration ttl);

    String findByUserId(UUID userId);

    void delete(UUID userId);

    void update(UUID userId, String newRefreshToken, Duration ttl);
}
