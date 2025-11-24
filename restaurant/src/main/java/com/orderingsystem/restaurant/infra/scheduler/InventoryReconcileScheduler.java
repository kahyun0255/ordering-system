package com.orderingsystem.restaurant.infra.scheduler;

import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@ConditionalOnProperty(prefix = "inventory.reconcile", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReconcileScheduler {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${key.stock}")
    private String stockKeyPrefix;

    @Value("${inventory.reconcile.page-size}")
    private int pageSize;

    @Value("${inventory.reconcile.lock}")
    private String RECONCILE_LOCK_KEY;

    @Value("${inventory.reconcile.ttl}")
    private long lockTtlMinutes;

    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private Duration LOCK_TTL;

    private String stockKey(UUID productId) {
        return stockKeyPrefix + productId;
    }

    @Scheduled(cron = "${inventory.reconcile.cron}")
    public void reconcileAll() {
        if (LOCK_TTL == null) {
            this.LOCK_TTL = Duration.ofMinutes(lockTtlMinutes);
        }

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(RECONCILE_LOCK_KEY, INSTANCE_ID, LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            log.info("다른 인스턴스가 inventory 동기화 작업을 실행 중이므로 동기화 실행 중지.");
            return;
        }

        try {
            log.info("inventory 동기화 작업 시작.");

            int pageNum = 0;
            int updatedCnt = 0;

            while (true) {
                final int currentPageNum = pageNum;
                Page<Product> page = transactionTemplate.execute(status ->
                        productRepository.findAll(PageRequest.of(currentPageNum, pageSize, Sort.by("productId")))
                );

                if (page == null || page.isEmpty()) {
                    break;
                }

                List<Product> products = page.getContent();

                List<String> keys = getProductKeys(products);
                List<Object> currentValues = getCurrentRedisValues(keys);
                List<String> desiredValues = getDesiredDbValues(products);

                List<Integer> diffIndexes = new ArrayList<>();
                for (int i = 0; i < products.size(); i++) {
                    String curStr = (String) currentValues.get(i);
                    String wantStr = desiredValues.get(i);
                    if (isQuantityMismatch(curStr, wantStr)) {
                        diffIndexes.add(i);
                    }
                }

                if (!diffIndexes.isEmpty()) {
                    updateRedisValuesPipelined(diffIndexes, keys, desiredValues);
                    updatedCnt += diffIndexes.size();
                }

                extendLockIfMine();

                if (!page.hasNext()) {
                    break;
                }
                pageNum++;
            }

            log.info("inventory 동기화 작업 완료. 업데이트된 key 수 : {}", updatedCnt);
        } catch (IllegalStateException e) {
            log.warn("inventory 동기화 작업 중단. {}", e.getMessage());
        } catch (Exception e) {
            log.error("inventory 동기화 작업 중 예외 발생. {}", e.getMessage(), e);
        } finally {
            releaseLock();
        }
    }

    private List<String> getProductKeys(List<Product> products) {
        List<String> keys = new ArrayList<>(products.size());
        for (Product p : products) {
            keys.add(stockKey(p.getProductId()));
        }
        return keys;
    }

    private List<Object> getCurrentRedisValues(List<String> keys) {
        return redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            StringRedisConnection c = (StringRedisConnection) conn;
            for (String k : keys) {
                c.get(k);
            }
            return null;
        });
    }

    private List<String> getDesiredDbValues(List<Product> products) {
        List<String> desiredValues = new ArrayList<>(products.size());
        for (Product p : products) {
            desiredValues.add(String.valueOf(p.getQuantity()));
        }
        return desiredValues;
    }

    private boolean isQuantityMismatch(String currentRedisValue, String desiredDbValue) {
        long cur;
        long want;

        try {
            cur = currentRedisValue == null ? -1L : Long.parseLong(currentRedisValue);
            want = Long.parseLong(desiredDbValue);
        } catch (NumberFormatException e) {
            return true;
        }
        return cur != want;
    }

    private void updateRedisValuesPipelined(List<Integer> diffIndexes, List<String> keys, List<String> desiredValues) {
        redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
            StringRedisConnection c = (StringRedisConnection) conn;
            for (int idx : diffIndexes) {
                c.set(keys.get(idx), desiredValues.get(idx));
            }
            return null;
        });
    }

    private void extendLockIfMine() {
        DefaultRedisScript<Long> expireIfMatch = new DefaultRedisScript<>();
        expireIfMatch.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('expire', KEYS[1], tonumber(ARGV[2])) " +
                        "else return 0 end"
        );
        expireIfMatch.setResultType(Long.class);

        Long r = redisTemplate.execute(
                expireIfMatch,
                Collections.singletonList(RECONCILE_LOCK_KEY),
                INSTANCE_ID,
                String.valueOf(LOCK_TTL.getSeconds())
        );
        if (r == null || r == 0L) {
            throw new IllegalStateException("Lock lost during processing");
        }
    }

    private void releaseLock() {
        DefaultRedisScript<Long> delIfMatch = new DefaultRedisScript<>();
        delIfMatch.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "  return redis.call('del', KEYS[1]) " +
                        "else return 0 end"
        );
        delIfMatch.setResultType(Long.class);

        try {
            redisTemplate.execute(delIfMatch, Collections.singletonList(RECONCILE_LOCK_KEY), INSTANCE_ID);
        } catch (Exception e) {
            log.warn("inventory 동기화 락 해재 실패. {}", e.getMessage());
        }
    }

}
