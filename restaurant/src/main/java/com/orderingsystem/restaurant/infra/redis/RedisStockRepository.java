package com.orderingsystem.restaurant.infra.redis;

import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.restaurant.application.StockCachePort;
import java.util.ArrayList;
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

    @Value("${stock.reserve.ttl-second}")
    private long reserveTtl;

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
                    "redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3])); " +
                    "redis.call('EXPIRE', KEYS[3], tonumber(ARGV[3])); " +
                    "return 1;";

    private static final String CONFIRM_LUA_SCRIPT =
            "for i = 1, #KEYS, 3 do " +
                    "  local stockKey = KEYS[i]; " +
                    "  local reserveKey = KEYS[i + 1]; " +
                    "  local historyKey = KEYS[i + 2]; " +
                    "  local quantity = tonumber(ARGV[(i - 1) / 3 + 1]); " +
                    "  local stock = tonumber(redis.call('GET', stockKey)); " +
                    "  local reserved = tonumber(redis.call('GET', reserveKey)); " +
                    "  if (not stock or not reserved) then return -1 end; " +
                    "  redis.call('SET', stockKey, stock - quantity); " +
                    "  redis.call('SET', reserveKey, math.max(0, reserved - quantity)); " +
                    "  redis.call('DEL', historyKey); " +
                    "end; " +
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
                productId.toString(),
                String.valueOf(reserveTtl)
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
        return redisTemplate.opsForHash().entries(historyKey(sagaId));
    }

    @Override
    public void confirm(Map<Object, Object> history, UUID sagaId) {
        if (history == null || history.isEmpty()) {
            log.warn("재고 예약 내역이 존재하지 않습니다. sagaId={}", sagaId);
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(CONFIRM_LUA_SCRIPT);
        script.setResultType(Long.class);

        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        history.forEach((productId, quantityStr) -> {
            UUID pid = UUID.fromString(productId.toString());
            keys.add(stockKey(pid));
            keys.add(reserveKey(pid));
            keys.add(historyKey(sagaId));
            args.add(quantityStr.toString());
        });

        Long result = redisTemplate.execute(script, keys, args.toArray());

        if (result == null || result == -1) {
            throw new IllegalStateException("재고 데이터가 존재하지 않습니다. " + history.keySet());
        }

        log.info("{} 주문 예약 확정 완료. Redis 실제 재고 차감 및 예약 해제", sagaId);
    }

    private int getInt(RedisOperations<String, String> ops, String key) {
        String val = ops.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

}
