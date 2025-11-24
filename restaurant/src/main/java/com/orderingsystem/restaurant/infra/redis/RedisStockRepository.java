package com.orderingsystem.restaurant.infra.redis;

import com.orderingsystem.restaurant.application.StockCachePort;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockRepository implements StockCachePort {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${key.stock}")
    private String stockKey;

    @Value("${key.reserved}")
    private String reserveKey;

    @Value("${key.confirmed}")
    private String confirmedKey;

    @Value("${stock.reserve.ttl-second}")
    private long reserveTtl;

    @Value("${key.history}")
    private String historyKey;

    private String reserveLua;
    private String confirmLua;
    private String cancelReservationLua;

    @PostConstruct
    public void init() throws IOException {
        reserveLua = loadScript("lua/reserve.lua");
        confirmLua = loadScript("lua/confirm.lua");
        cancelReservationLua = loadScript("lua/cancelReservation.lua");
    }

    private String loadScript(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String stockKey(UUID productId) {
        return stockKey + productId;
    }

    private String reserveKey(UUID productId) {
        return reserveKey + productId;
    }

    private String historyKey(UUID sagaId) {
        return historyKey + sagaId;
    }

    private String confirmedKey(UUID sagaId) {
        return confirmedKey + sagaId;
    }

    @Override
    public void reserve(UUID productId, int quantity, UUID sagaId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(reserveLua);
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
    public void confirm(Map<Object, Object> history, UUID sagaId, UUID orderId) {
        if (history == null || history.isEmpty()) {
            log.warn("재고 예약 내역이 존재하지 않습니다. sagaId={}", sagaId);
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(confirmLua);
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

        System.out.println("<<<<  " + confirmedKey(orderId) + " " + history.toString());
        redisTemplate.opsForHash().putAll(confirmedKey(orderId), history);

        log.info("{} 주문 예약 확정 완료. Redis 실제 재고 차감 및 예약 해제", sagaId);
    }

    @Override
    public void cancelReservation(Map<Object, Object> history, UUID sagaId) {
        if (history == null || history.isEmpty()) {
            log.warn("재고 예약 내역이 존재하지 않습니다. sagaId={}", sagaId);
            return;
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(cancelReservationLua);
        script.setResultType(Long.class);

        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        history.forEach((productId, quantityStr) -> {
            UUID pid = UUID.fromString(productId.toString());
            keys.add(reserveKey(pid));
            keys.add(historyKey(sagaId));
            args.add(quantityStr.toString());
        });

        Long result = redisTemplate.execute(script, keys, args.toArray());

        if (result == null || result == -1) {
            throw new IllegalStateException("재고 데이터가 존재하지 않습니다. " + history.keySet());
        }

        log.info("{} 주문 예약 취소 완료. Redis 예약 수량 복구 및 히스토리 삭제.", sagaId);
    }

    @Override
    public Map<Object, Object> getConfirmed(UUID orderId) {
        return redisTemplate.opsForHash().entries(confirmedKey(orderId));
    }

    @Override
    public void restoreConfirmed(Map<Object, Object> confirmed, UUID orderId) {
        if (confirmed == null || confirmed.isEmpty()) {
            return;
        }

        confirmed.forEach((pid, qty) -> {
            UUID productId = UUID.fromString(pid.toString());
            redisTemplate.opsForValue().increment(stockKey(productId), Long.parseLong(qty.toString()));
        });

        redisTemplate.delete(confirmedKey(orderId));

        log.info("[{}] 주문 거절/취소에 따른 재고 복구 완료.", orderId);
    }

    @Override
    public void update(UUID productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 id는 null이 불가능합니다.");
        }

        if (quantity < 0) {
            log.info("재고 수량은 음수가 될 수 없습니다. productId : {}, quantity : {}", productId, quantity);
            throw new IllegalArgumentException("재고 수량은 음수가 될 수 없습니다.");
        }

        String stockKey = stockKey(productId);

        try {
            redisTemplate.opsForValue().set(stockKey, String.valueOf(quantity));
            log.info("Redis 재고 갱신/생성 완료. productId : {}, quantity : {}", productId, quantity);
        } catch (Exception e) {
            log.error("Redis 재고 갱신/생성 중 오류 발생. productId : {}, quantity : {}", productId, quantity);
            throw new IllegalStateException("Redis 재고 갱신 실패.", e);
        }
    }

    @Override
    public void delete(UUID productId) {
        try {
            redisTemplate.delete(stockKey(productId));
            redisTemplate.delete(reserveKey(productId));
            log.info("Redis 재고 및 예약 데이터 삭제 완료. product Id : {}", productId);
        } catch (Exception e) {
            log.error("Redis 재고 삭제 중 오류 발생. product Id : {}, error : {}", productId, e.getMessage());
            throw new IllegalStateException("Redis 재고 삭제 중 오류 발생.", e);
        }
    }

}
