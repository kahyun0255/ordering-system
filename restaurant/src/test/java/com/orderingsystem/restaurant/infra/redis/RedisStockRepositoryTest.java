package com.orderingsystem.restaurant.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.util.RedisTransaction;
import java.util.Map;
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

    private UUID productId;
    private UUID productId2;
    private UUID sagaId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        sagaId = UUID.randomUUID();

        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(stockKey + productId, "10");
        ops.set(stockKey + productId2, "20");

        redisTemplate.delete(reserveKey + productId);
        redisTemplate.delete(reserveKey + productId2);
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
        redisTemplate.opsForValue().set(reserveKey + productId, "0");

        //when
        redisStockRepository.reserve(productId, 1, sagaId);

        //then
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(reserved).isEqualTo("1");
    }

    @DisplayName("한개의 상품을 주문 할 경우, 레스토랑 승인에 성공하면 재고 차감, 예약 해제, 히스토리 삭제가 이루어진다.")
    @Test
    void testConfirmSingleSuccess() {
        //given
        redisTemplate.opsForValue().set(stockKey + productId, "10");
        redisTemplate.opsForValue().set(reserveKey + productId, "3");
        redisTemplate.opsForHash().put(historyKey + sagaId, productId.toString(), "3");

        Map<Object, Object> history = redisStockRepository.getHistory(sagaId);

        //when
        redisStockRepository.confirm(history, sagaId);

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey + productId)).isEqualTo("7");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId)).isEqualTo("0");
        assertThat(redisTemplate.hasKey(historyKey + sagaId)).isFalse();
    }

    @DisplayName("여러개의 상품을 주문 할 경우, 레스토랑 승인에 성공하면 각각의 상품에 대해 재고 차감, 예약 해제, 히스토리 삭제가 이루어진다.")
    @Test
    void testConfirmMultipleProducts() {
        //given
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        redisTemplate.opsForValue().set(stockKey + productId1, "10");
        redisTemplate.opsForValue().set(stockKey + productId2, "20");
        redisTemplate.opsForValue().set(reserveKey + productId1, "5");
        redisTemplate.opsForValue().set(reserveKey + productId2, "7");

        redisTemplate.opsForHash().put(historyKey + sagaId, productId1.toString(), "5");
        redisTemplate.opsForHash().put(historyKey + sagaId, productId2.toString(), "7");

        Map<Object, Object> history = redisStockRepository.getHistory(sagaId);

        //when
        redisStockRepository.confirm(history, sagaId);

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey + productId1)).isEqualTo("5");
        assertThat(redisTemplate.opsForValue().get(stockKey + productId2)).isEqualTo("13");

        assertThat(redisTemplate.opsForValue().get(reserveKey + productId1)).isEqualTo("0");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId2)).isEqualTo("0");

        assertThat(redisTemplate.hasKey(historyKey + sagaId)).isFalse();
    }

    @DisplayName("히스토리가 비어있을 경우 아무 동작도 하지 않는다.")
    @Test
    void testConfirmEmptyHistory() {
        //given
        Map<Object, Object> history = redisStockRepository.getHistory(sagaId);

        //when
        redisStockRepository.confirm(history, sagaId);

        //then
        assertThat(redisTemplate.hasKey(historyKey + sagaId)).isFalse();
    }

    @DisplayName("재고 키가 존재하지 않으면 예외가 발생한다.")
    @Test
    void testConfirmMissingStockKey() {
        //given
        UUID missingProductId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        redisTemplate.opsForHash().put(historyKey + sagaId, missingProductId.toString(), "2");

        Map<Object, Object> history = redisStockRepository.getHistory(sagaId);

        //when, then
        assertThatThrownBy(() -> redisStockRepository.confirm(history, sagaId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 데이터가 존재하지 않습니다. [" + missingProductId + "]");
    }

    @DisplayName("Saga Id 기준으로 예약을 취소하면 예약 수량이 감소하고 히스토리가 삭제된다.")
    @Test
    void testCancelReservation() {
        //given
        redisTemplate.opsForValue().set(reserveKey + productId, "3");
        redisTemplate.opsForValue().set(reserveKey + productId2, "10");

        redisTemplate.opsForHash().put(historyKey + sagaId, productId.toString(), "3");
        redisTemplate.opsForHash().put(historyKey + sagaId, productId2.toString(), "5");

        Map<Object, Object> history = redisStockRepository.getHistory(sagaId);

        //when
        redisStockRepository.cancelReservation(history, sagaId);

        //then
        String reserved1 = redisTemplate.opsForValue().get(reserveKey + productId);
        String reserved2 = redisTemplate.opsForValue().get(reserveKey + productId2);
        assertThat(reserved1).isEqualTo("0");
        assertThat(reserved2).isEqualTo("5");

        assertThat(redisTemplate.hasKey(historyKey + sagaId)).isFalse();
    }

    @DisplayName("재고 업데이트시 Redis에 기존 상품이 존재하지 않으면 새로운 수량이 반영된다.")
    @Test
    void testUpdateStockSuccess() {
        //given
        UUID newProductId = UUID.randomUUID();

        //when
        redisStockRepository.update(newProductId, 10);

        //then
        String updateStock = redisTemplate.opsForValue().get(stockKey + newProductId);
        assertThat(updateStock).isEqualTo("10");
    }

    @DisplayName("재고 업데이트시 Redis에 기존 상품이 존재하면 새로운 수량이 반영된다.")
    @Test
    void testUpdateStockSuccess2() {
        //when
        redisStockRepository.update(productId, 1000000);

        //then
        String updateStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(updateStock).isEqualTo("1000000");
    }

    @DisplayName("업데이트 할 재고가 음수라면 예외가 발생한다.")
    @Test
    void testUpdateNegativeStock() {
        //when, then
        assertThatThrownBy(() -> redisStockRepository.update(productId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고 수량은 음수가 될 수 없습니다.");
    }

    @DisplayName("업데이트 할 productId가 null이라면 예외가 발생한다.")
    @Test
    void testUpdateNullProductId() {
        //when, then
        assertThatThrownBy(() -> redisStockRepository.update(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 id는 null이 불가능합니다.");
    }

    @DisplayName("상품 삭제시 Redis 재고 및 예약 키가 모두 삭제된다.")
    @Test
    void testDeleteStockSuccess() {
        //given
        UUID deleteId = UUID.randomUUID();
        redisTemplate.opsForValue().set(stockKey + deleteId, "50");
        redisTemplate.opsForValue().set(reserveKey + deleteId, "50");

        //when
        redisStockRepository.delete(deleteId);

        //then
        Boolean stockExists = redisTemplate.hasKey(stockKey + deleteId);
        Boolean reserveExists = redisTemplate.hasKey(reserveKey + deleteId);

        assertThat(stockExists).isFalse();
        assertThat(reserveExists).isFalse();
    }

    @DisplayName("Redis에 해당 상품 키가 없어도 삭제 시 예외가 발생하지 않는다.")
    @Test
    void testDeleteNonExistingKeys() {
        //given
        UUID unknownId = UUID.randomUUID();

        //when
        redisStockRepository.delete(unknownId);

        //then
        Boolean stockExists = redisTemplate.hasKey(stockKey + unknownId);
        Boolean reserveExists = redisTemplate.hasKey(reserveKey + unknownId);

        assertThat(stockExists).isFalse();
        assertThat(reserveExists).isFalse();
    }

}
