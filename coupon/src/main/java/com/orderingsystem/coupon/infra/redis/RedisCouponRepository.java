package com.orderingsystem.coupon.infra.redis;

import com.orderingsystem.coupon.application.port.out.CouponCachePort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RedisCouponRepository implements CouponCachePort {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${coupon.redis.stock-prefix}")
    private String stockPrefix;

    @Value("${coupon.redis.issued-prefix}")
    private String issuedPrefix;

    private String stockKey(UUID couponId) {
        return stockPrefix + couponId;
    }

    private String issuedKey(UUID couponId) {
        return issuedPrefix + couponId;
    }

    @Override
    public long currentStock(UUID couponId) {
        String stock = redisTemplate.opsForValue().get(stockKey(couponId));
        return stock == null ? 0L : Long.parseLong(stock);
    }

    @Override
    public Long decreaseStock(UUID couponId) {
        return redisTemplate.opsForValue().decrement(stockKey(couponId));
    }

    @Override
    public Long increaseStock(UUID couponId) {
        return redisTemplate.opsForValue().increment(stockKey(couponId));
    }

    @Override
    public boolean addIssuedUser(UUID couponId, UUID userId) {
        Long result = redisTemplate.opsForSet().add(issuedKey(couponId), userId.toString());
        return result != null && result == 1L;
    }

    @Override
    public void removeIssuedUser(UUID couponId, UUID userId) {
        redisTemplate.opsForSet().remove(issuedKey(couponId), userId.toString());
    }

    @Override
    public boolean exists(UUID couponId) {
        return redisTemplate.hasKey(stockKey(couponId));
    }

}
