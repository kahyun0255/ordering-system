package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.util.RedisTransaction;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestaurantStockIntegrationTest {

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
    private RestaurantStockFacade restaurantStockFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${key.stock}")
    private String stockKey;

    @Value("${key.reserved}")
    private String reserveKey;

    @Value("${key.history}")
    private String historyKey;

    private UUID productId;
    private UUID productId2;
    private UUID productId3;
    private UUID sagaId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        productId3 = UUID.randomUUID();
        sagaId = UUID.randomUUID();

        Product product = Product.builder()
                .productId(productId)
                .name("상품")
                .price(new Money(BigDecimal.valueOf(1000)))
                .quantity(10)
                .available(true)
                .build();
        Product product2 = Product.builder()
                .productId(productId2)
                .name("상품2")
                .price(new Money(BigDecimal.valueOf(2000)))
                .quantity(12)
                .available(true)
                .build();
        Product product3 = Product.builder()
                .productId(productId3)
                .name("상품3")
                .price(new Money(BigDecimal.valueOf(3000)))
                .quantity(13)
                .available(true)
                .build();
        productRepository.saveAll(List.of(product, product2, product3));

        redisTemplate.opsForValue().set(stockKey + productId, "10");
        redisTemplate.opsForValue().set(stockKey + productId2, "12");
        redisTemplate.opsForValue().set(stockKey + productId3, "13");

        redisTemplate.delete(reserveKey + productId);
        redisTemplate.delete(reserveKey + productId2);
        redisTemplate.delete(reserveKey + productId3);

        redisTemplate.delete(historyKey + sagaId);
    }

    @DisplayName("주문 검증시 재고를 예약하면 Redis에 예약 수량이 반영된다.")
    @Test
    void reserveStock() {
        //when
        restaurantStockFacade.reserve(productId, 3, sagaId);

        //then
        String totalStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(totalStock).isEqualTo("10");

        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);
        assertThat(reserved).isEqualTo("3");

        String history = (String) redisTemplate.opsForHash().get(historyKey + sagaId, productId.toString());
        assertThat(history).isEqualTo("3");
    }

    @DisplayName("주문 승인시 Redis 재고가 차감되고 DB 재고도 감소한다.")
    @Test
    void confirmStock() {
        //given
        restaurantStockFacade.reserve(productId, 4, sagaId);

        //when
        restaurantStockFacade.confirm(sagaId);

        //then
        String totalStock = redisTemplate.opsForValue().get(stockKey + productId);
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);
        Map<Object, Object> history = redisTemplate.opsForHash().entries(historyKey + sagaId);

        assertThat(totalStock).isEqualTo("6");
        assertThat(reserved).isEqualTo("0");
        assertThat(history).isEmpty();

        Product after = productRepository.findById(productId).orElseThrow();
        assertThat(after.getQuantity()).isEqualTo(6);
    }

    @DisplayName("재고 예약이 없을 때 confirm()을 호출하면 아무 일도 일어나지 않는다.")
    @Test
    void confirmWithoutHistory() {
        //when
        restaurantStockFacade.confirm(sagaId);

        //then
        Product after = productRepository.findById(productId).orElseThrow();
        assertThat(after.getQuantity()).isEqualTo(10);
    }

    @DisplayName("예약 수량보다 재고가 적으면 예외가 발생한다.")
    @Test
    void reserveMoreThanStockShouldThrow() {
        //given
        redisTemplate.opsForValue().set(stockKey + productId, "2");

        //when, then
        assertThatThrownBy(() -> restaurantStockFacade.reserve(productId, 5, sagaId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고 부족");
    }

    @DisplayName("하나의 주문에서 여러 상품을 예약하면 각각의 Redis 예약 수량이 반영된다.")
    @Test
    void reserveMultipleProducts() {
        //when
        restaurantStockFacade.reserve(productId, 2, sagaId);
        restaurantStockFacade.reserve(productId2, 3, sagaId);
        restaurantStockFacade.reserve(productId3, 4, sagaId);

        //then
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId)).isEqualTo("2");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId2)).isEqualTo("3");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId3)).isEqualTo("4");

        assertThat(redisTemplate.opsForHash().get(historyKey + sagaId, productId.toString())).isEqualTo("2");
        assertThat(redisTemplate.opsForHash().get(historyKey + sagaId, productId2.toString())).isEqualTo("3");
        assertThat(redisTemplate.opsForHash().get(historyKey + sagaId, productId3.toString())).isEqualTo("4");
    }

    @DisplayName("여러 상품 예약 후 confirm시 Redis, DB에서 각각 재고가 올바르게 차감된다.")
    @Test
    void confirmMultipleProducts() {
        //given
        restaurantStockFacade.reserve(productId, 2, sagaId);
        restaurantStockFacade.reserve(productId2, 4, sagaId);
        restaurantStockFacade.reserve(productId3, 6, sagaId);

        //when
        restaurantStockFacade.confirm(sagaId);

        //then
        assertThat(redisTemplate.opsForValue().get(stockKey + productId)).isEqualTo("8");
        assertThat(redisTemplate.opsForValue().get(stockKey + productId2)).isEqualTo("8");
        assertThat(redisTemplate.opsForValue().get(stockKey + productId3)).isEqualTo("7");

        assertThat(redisTemplate.opsForValue().get(reserveKey + productId)).isEqualTo("0");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId2)).isEqualTo("0");
        assertThat(redisTemplate.opsForValue().get(reserveKey + productId3)).isEqualTo("0");

        assertThat(redisTemplate.opsForHash().entries(historyKey + sagaId)).isEmpty();

        Optional<Product> afterProduct1 = productRepository.findById(productId);
        assertThat(afterProduct1.get().getQuantity()).isEqualTo(8);

        Optional<Product> afterProduct2 = productRepository.findById(productId2);
        assertThat(afterProduct2.get().getQuantity()).isEqualTo(8);

        Optional<Product> afterProduct3 = productRepository.findById(productId3);
        assertThat(afterProduct3.get().getQuantity()).isEqualTo(7);
    }

    @DisplayName("TTL이 만료되면 예약된 재고와 히스토리가 자동으로 삭제된다.")
    @Test
    void reserveStockWithTTL() throws InterruptedException {
        //given
        restaurantStockFacade.reserve(productId, 2, sagaId);

        redisTemplate.expire(reserveKey + productId, java.time.Duration.ofSeconds(2));
        redisTemplate.expire(historyKey + sagaId, java.time.Duration.ofSeconds(2));

        //when
        Thread.sleep(2500);

        //then
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);
        Map<Object, Object> history = redisTemplate.opsForHash().entries(historyKey + sagaId);

        assertThat(reserved).isNull();
        assertThat(history).isEmpty();

        String totalStock = redisTemplate.opsForValue().get(stockKey + productId);
        assertThat(totalStock).isEqualTo("10");
    }

    @DisplayName("Saga ID 기준으로 예약을 취소하면 Redis의 예약 수량이 복구되고 히스토리가 삭제된다.")
    @Test
    void cancelReservationBySagaId() {
        //given
        redisTemplate.opsForValue().set(stockKey + productId, "10");
        redisTemplate.opsForValue().set(stockKey + productId2, "20");
        redisTemplate.opsForValue().set(reserveKey + productId, "3");
        redisTemplate.opsForValue().set(reserveKey + productId2, "5");

        redisTemplate.opsForHash().put(historyKey + sagaId, productId.toString(), "3");
        redisTemplate.opsForHash().put(historyKey + sagaId, productId2.toString(), "3");

        //when
        restaurantStockFacade.cancelReservation(sagaId);

        //then
        String reserved1 = redisTemplate.opsForValue().get(reserveKey + productId);
        String reserved2 = redisTemplate.opsForValue().get(reserveKey + productId2);

        assertThat(reserved1).isEqualTo("0");
        assertThat(reserved2).isEqualTo("2");
        assertThat(redisTemplate.opsForHash().entries(historyKey + sagaId)).isEmpty();
    }

    @DisplayName("히스토리가 없을 때 호출하면 아무 일도 일어나지 않는다.")
    @Test
    void cancelReservationWithoutHistory() {
        //when, then
        assertThatNoException().isThrownBy(()->restaurantStockFacade.cancelReservation(sagaId));
    }

}
