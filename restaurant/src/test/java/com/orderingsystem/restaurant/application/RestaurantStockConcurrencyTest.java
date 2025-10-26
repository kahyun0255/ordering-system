package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.restaurant.infra.redis.RedisStockRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RestaurantStockConcurrencyTest {

    @Autowired
    private ProductStockFacade productStockFacade;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisStockRepository redisStockRepository;

    @Value("${key.stock}")
    private String stockKey;

    @Value("${key.reserved}")
    private String reserveKey;

    @Value("${key.history}")
    private String historyKey;

    private final UUID productId = UUID.randomUUID();

    @DisplayName("여러 스레드가 동시에 같은 상품 재고를 예약해도 일관성 있게 처리된다.")
    @Test
    void concurrentReserveTest() throws InterruptedException {
        //given
        int treadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(treadCount);
        CountDownLatch latch = new CountDownLatch(treadCount);

        redisTemplate.opsForValue().set(stockKey + productId, "100");

        //when
        for (int i = 0; i < treadCount; i++) {
            executor.submit(() -> {
                try {
                    productStockFacade.reserve(productId, 3, UUID.randomUUID());
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        //then
        String totalStock = redisTemplate.opsForValue().get(stockKey + productId);
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);

        assertThat(totalStock).isEqualTo("100");
        assertThat(reserved).isEqualTo("30");
    }

    @DisplayName("여러 스레드가 동시에 같은 상품을 판매해도 일관성 있게 처리된다.")
    @Test
    void concurrentConfirmTest() throws InterruptedException {
        //given
        int treadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(treadCount);
        CountDownLatch latch = new CountDownLatch(treadCount);

        redisTemplate.opsForValue().set(stockKey + productId, "100");

        //when
        for (int i = 0; i < treadCount; i++) {
            executor.submit(() -> {
                UUID sagaId = UUID.randomUUID();
                try {
                    productStockFacade.reserve(productId, 3, sagaId);
                    productStockFacade.confirm(sagaId);
                } catch (Exception e) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        //then
        String totalStock = redisTemplate.opsForValue().get(stockKey + productId);
        String reserved = redisTemplate.opsForValue().get(reserveKey + productId);

        assertThat(totalStock).isEqualTo("70");
        assertThat(reserved).isEqualTo("0");
    }

}
