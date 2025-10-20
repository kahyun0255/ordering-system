package com.orderingsystem.restaurant.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.util.RedisTransaction;
import java.util.UUID;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RedisStockRepositoryTest {

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
    private RedisStockRepository redisStockRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisTransaction redisTransaction;

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

    private UUID productId;
    private UUID sagaId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        sagaId = UUID.randomUUID();
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(stockKey + productId, "10");
        redisTemplate.delete(reserveKey + productId);
    }

    @DisplayName("정상적으로 재고가 예약된다.")
    @Test
    void testReserveSuccess() {
        //when
        redisStockRepository.reserve(productId, 3, sagaId);

        //then
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(reserved).isEqualTo("3");

        String history = (String) redisTemplate.opsForHash().get(historyKey + sagaId, productId.toString());
        assertThat(history).isEqualTo("3");

    }

    @DisplayName("재고가 부족할 경우 예외가 발생한다.")
    @Test
    void testReserveInsufficientStock() {
        //given
        redisTemplate.opsForValue().set(stockKey + productId, "2");

        //when, then
        assertThatThrownBy(() -> redisStockRepository.reserve(productId, 5, sagaId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 부족");
    }

    @DisplayName("락이 걸린 상태에서는 재고 수정 중 예외가 발생한다.")
    @Test
    void testReserveLockConflict() {
        //given
        redisTemplate.opsForValue().set(stockKey + productId, "1");
        redisTemplate.opsForValue().set(lockKey + productId, "1");

        //when, then
        assertThatThrownBy(() -> redisStockRepository.reserve(productId, 1, sagaId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 수정 중");
    }

}
