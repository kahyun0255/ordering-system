package com.orderingsystem.restaurant.infra.redis;

import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.restaurant.application.StockCachePort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    private static final String RESERVE_LUA_SCRIPT =
            "local stock = redis.call('GET', KEYS[1]); " +
                    "if (not stock) then return -1 end; " +
                    "local reserved = redis.call('GET', KEYS[2]); " +
                    "if (not reserved) then reserved = 0 end; " +
                    "if (tonumber(stock) - tonumber(reserved) < tonumber(ARGV[1])) then return 0 end; " +
                    "redis.call('INCRBY', KEYS[2], ARGV[1]); " +
                    "redis.call('HSET', KEYS[3], ARGV[2], ARGV[1]); " +
                    "return 1;";

    private String stockKey(UUID productId) {
        return stockKey + productId;
    }

    private String reserveKey(UUID productId) {
        return reserveKey + productId;
    }

    private String historyKey(UUID sagaId) {
        return historyKey + sagaId;
    }

    @Override
    public void reserve(UUID productId, int quantity, UUID sagaId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RESERVE_LUA_SCRIPT);
        script.setResultType(Long.class);

        String stock = stockKey(productId);
        String reserved = reserveKey(productId);
        String history = historyKey(sagaId);

        Long result = redisTemplate.execute(
                script,
                List.of(stock, reserved, history),
                String.valueOf(quantity),
                productId.toString()
        );

        if (result == null || result == -1) {
            throw new IllegalStateException("재고 키가 존재하지 않습니다.");
        }
        if (result == 0) {
            throw new IllegalStateException("재고 부족");
        }

        log.info("[{}] 상품 {} 예약 완료.", productId, quantity);
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
