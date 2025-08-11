package com.orderingsystem.infra.redis;

import com.orderingsystem.domain.repository.RefreshTokenRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(UUID userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    @Override
    public String findByUserId(UUID userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    @Override
    public void delete(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    @Override
    public void update(UUID userId, String newRefreshToken, Duration ttl) {
        delete(userId);
        save(userId, newRefreshToken, ttl);
    }

    private String key(UUID userId) {
        return "refreshToken:" + userId.toString();
    }
}
