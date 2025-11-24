package com.orderingsystem.restaurant.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "key.stock=stock:",
        "key.reserved=stock:reserved:",
        "key.confirmed=stock:confirmed:",
        "key.history=stock:history:",
        "stock.reserve.ttl-second=60"
})
class InventoryReconcileSchedulerIntegrationTest {

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        public RedisTransaction redisTransaction() {
            return new RedisTransaction() {
                @Override
                public void execute(RedisTemplate<String, String> redisTemplate,
                                    java.util.function.Consumer<RedisOperations<String, String>> callback) {
                    callback.accept(redisTemplate);
                }
            };
        }
    }

    @Autowired
    private InventoryReconcileScheduler inventoryReconcileScheduler;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${key.stock}")
    private String stockKeyPrefix;

    @Value("${inventory.reconcile.lock}")
    private String lockKey;

    private String stockKey(UUID id) {
        return stockKeyPrefix + id;
    }

    @BeforeEach
    void setUp() {
        Set<String> keys = stringRedisTemplate.keys(stockKeyPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        stringRedisTemplate.delete(lockKey);

    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
    }

    @DisplayName("락을 획득하면 DB-Redis 불일치 키만 갱신한다.")
    @Test
    void shouldSyncOnlyMismatchedKeys_whenLockIsAcquired() {
        //given
        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .productId(productId)
                .name("상품")
                .available(true)
                .price(new Money(BigDecimal.valueOf(1000)))
                .quantity(10)
                .build());

        stringRedisTemplate.opsForValue().set(stockKey(productId), "7");

        //when
        inventoryReconcileScheduler.reconcileAll();

        //then
        assertThat(stringRedisTemplate.opsForValue().get(stockKey(productId))).isEqualTo("10");
    }

    @DisplayName("다른 인스턴스가 락을 보유중이면 동기화를 실행하지 않는다.")
    @Test
    void shouldNotRunSync_whenLockIsHeldByAnotherInstance() {
        //given
        stringRedisTemplate.opsForValue().set(lockKey, "other", Duration.ofMinutes(5));

        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .productId(productId)
                .name("상품")
                .available(true)
                .price(new Money(BigDecimal.valueOf(1000)))
                .quantity(10)
                .build());

        stringRedisTemplate.opsForValue().set(stockKey(productId), "0");

        //when
        inventoryReconcileScheduler.reconcileAll();

        //then
        assertThat(stringRedisTemplate.opsForValue().get(stockKey(productId))).isEqualTo("0");
    }

}
