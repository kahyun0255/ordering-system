package com.orderingsystem.restaurant.infra.redis;

import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.restaurant.application.StockCachePort;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockRepository implements StockCachePort {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTransaction redisTransaction;

    @Value("${key.stock}")
    private String stockKey;

    @Value("${key.reserved}")
    private String reserveKey;

    @Value("${key.lock}")
    private String lockKey;

    @Value("${key.history}")
    private String historyKey;

    @Value("${lock.expire-second}")
    private int lockExpire;

    private String stockKey(UUID productId) {
        return stockKey + productId;
    }

    private String reserveKey(UUID productId) {
        return reserveKey + productId;
    }

    private String lockKey(UUID productId) {
        return lockKey + productId;
    }

    @Override
    public void reserve(UUID productId, int quantity, UUID sagaId) {
        String lock = lockKey(productId);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lock, "1", Duration.ofSeconds(lockExpire));
        if (locked.equals(Boolean.FALSE)) {
            throw new IllegalStateException("재고 수정 중");
        }

        try {
            redisTransaction.execute(redisTemplate, ops -> {
                int total = getInt(ops, stockKey(productId));
                int reserved = getInt(ops, reserveKey(productId));

                if (total - reserved < quantity) {
                    throw new IllegalStateException("재고 부족");
                }

                ops.opsForValue().increment(reserveKey(productId), quantity);
                ops.opsForHash().put(historyKey + sagaId, productId.toString(), String.valueOf(quantity));
            });

            log.info("[{}] 상품 {} 예약 완료.", productId, quantity);
        } finally {
            redisTemplate.delete(lock);
        }
    }

    @Override
    public Map<Object, Object> getHistory(UUID sagaId) {
        return redisTemplate.opsForHash().entries(historyKey + sagaId);
    }

    @Override
    public void confirm(Map<Object, Object> history, UUID sagaId) {
        redisTransaction.execute(redisTemplate, ops -> {
            history.forEach((productId, quantityStr) -> {
                int quantity = Integer.parseInt(quantityStr.toString());
                String productKey = stockKey(UUID.fromString(productId.toString()));
                String reservedKey = reserveKey(UUID.fromString(productId.toString()));

                int total = getInt(ops, productKey);
                int reserved = getInt(ops, reservedKey);

                if (total == 0 && reserved == 0) {
                    throw new IllegalStateException("재고 데이터가 존재하지 않습니다." + productId);
                }

                ops.opsForValue().set(productKey, String.valueOf(total - quantity));
                ops.opsForValue().set(reservedKey, String.valueOf(Math.max(0, reserved - quantity)));
            });

            ops.delete(historyKey + sagaId);
        });

        log.info("{} 주문 예약 확정 완료. Redis 실제 재고 차감 및 예약 해제", sagaId);
    }

    private int getInt(RedisOperations<String, String> ops, String key) {
        String val = ops.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

}
