package com.orderingsystem.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import com.orderingsystem.coupon.application.port.out.CouponIssueMessagePublisher;
import com.orderingsystem.coupon.infra.redis.RedisCouponRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public class IssueCouponServiceConcurrencyTest {

    @Autowired
    private IssueCouponService issueCouponService;

    @Autowired
    private RedisCouponRepository redisCouponRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private CouponIssueMessagePublisher couponIssueMessagePublisher;

    @DisplayName("재고가 100개일 때 1000명이 동시에 요청하면 정확히 100개만 발급되어야 한다.")
    @Test
    void shouldIssueExactly100Coupons_when1000ConcurrentRequestsAndStockIs100() throws InterruptedException {
        //given
        int threadCount = 1000;
        Long issueLimit = 100L;
        UUID couponId = UUID.randomUUID();

        redisCouponRepository.enableCoupon(couponId, issueLimit, LocalDateTime.now().plusDays(1));

        doNothing().when(couponIssueMessagePublisher).publish(any());

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    issueCouponService.requestCouponIssuance(couponId, UUID.randomUUID(), LocalDateTime.now());
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        assertThat(successCount.get()).isEqualTo(issueLimit.intValue());
        assertThat(failCount.get()).isEqualTo(threadCount - issueLimit);

        long currentStock = redisCouponRepository.currentStock(couponId);
        assertThat(currentStock).isEqualTo(0L);
    }

    @DisplayName("같은 유저가 동시에 여러 번 요청해도 쿠폰은 한 번만 발급되어야 한다.")
    @Test
    void shouldIssueCouponOnlyOnce_whenSameUserRequestsConcurrently() throws InterruptedException {
        //given
        int threadCount = 5;
        Long issueLimit = 10L;
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        redisCouponRepository.enableCoupon(couponId, issueLimit, LocalDateTime.now().plusDays(1L));

        doNothing().when(couponIssueMessagePublisher).publish(any());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    issueCouponService.requestCouponIssuance(couponId, userId, LocalDateTime.now());
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        //then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        long currentStock = redisCouponRepository.currentStock(couponId);
        assertThat(currentStock).isEqualTo(issueLimit - 1);
    }

}
