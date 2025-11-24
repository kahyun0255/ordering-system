package com.orderingsystem.restaurant.infra.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class InventoryReconcileSchedulerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private InventoryReconcileScheduler inventoryReconcileScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryReconcileScheduler, "stockKeyPrefix", "stock:");
        ReflectionTestUtils.setField(inventoryReconcileScheduler, "pageSize", 100);
        ReflectionTestUtils.setField(inventoryReconcileScheduler, "RECONCILE_LOCK_KEY", "lock:reconcile");
        ReflectionTestUtils.setField(inventoryReconcileScheduler, "lockTtlMinutes", 5L);

        given(stringRedisTemplate.opsForValue()).willReturn(valueOps);
    }

    @DisplayName("락을 획득하면 DB-Redis 불일치 키만 갱신한다. (파이프라인 GET/SET 호출 검증)")
    @Test
    void shouldUpdateOnlyMismatchedKeys_whenLockIsAcquired() {
        //given
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

        given(transactionTemplate.execute(any()))
                .willAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        // extendLockIfMine()
        given(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any()))
                .willReturn(1L);
        // releaseLock()
        given(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .willReturn(1L);

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        Product product1 = Product.builder()
                .productId(productId1)
                .name("상품 1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .quantity(10)
                .available(true)
                .build();

        Product product2 = Product.builder()
                .productId(productId2)
                .name("상품 2")
                .price(new Money(BigDecimal.valueOf(1000)))
                .quantity(100)
                .available(true)
                .build();

        Page<Product> page1 = new PageImpl<>(List.of(product1, product2), PageRequest.of(0, 100, Sort.by("productId")),
                2);

        given(productRepository.findAll(any(PageRequest.class))).willReturn(page1).willReturn(Page.empty());

        given(stringRedisTemplate.executePipelined(any(RedisCallback.class)))
                .willReturn(List.of("0", "100")).willReturn(Collections.emptyList());

        //when
        inventoryReconcileScheduler.reconcileAll();

        //then
        verify(stringRedisTemplate, times(2)).executePipelined(any(RedisCallback.class));

        verify(stringRedisTemplate, atLeastOnce()).execute(any(DefaultRedisScript.class), anyList(), any(), any());
        verify(stringRedisTemplate, atLeastOnce()).execute(any(DefaultRedisScript.class), anyList(), any());
    }

    @DisplayName("락을 획득하지 못하면 작업을 즉시 종료한다. (DB 조회, Redis 파이프라인을 호출하지 않는다.)")
    @Test
    void shouldExitEarly_whenLockIsNotAcquired() {
        //given
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

        //when
        inventoryReconcileScheduler.reconcileAll();

        //then
        verifyNoInteractions(productRepository);
        verify(stringRedisTemplate, never()).executePipelined(any(RedisCallback.class));
        verify(stringRedisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any(), any());
        verify(stringRedisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), any());
    }

}
