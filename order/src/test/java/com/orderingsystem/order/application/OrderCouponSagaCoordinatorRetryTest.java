package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.order.application.dto.response.CouponResponse;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(OrderCouponSagaCoordinatorRetryTest.RetryTestConfig.class)
class OrderCouponSagaCoordinatorRetryTest {

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    static class RetryTestConfig {

        @Bean
        OrderCouponService orderCouponService() {
            return mock(OrderCouponService.class);
        }

        @Bean
        OrderCouponSagaCoordinator orderCouponSagaCoordinator(OrderCouponService orderCouponService) {
            return new OrderCouponSagaCoordinator(orderCouponService);
        }
    }

    @Resource
    private OrderCouponService orderCouponService;

    @Resource
    private OrderCouponSagaCoordinator orderCouponSagaCoordinator;

    private CouponResponse couponResponse;

    @BeforeEach
    void setUp() {
        couponResponse = CouponResponse.builder().build();
        org.mockito.Mockito.reset(orderCouponService);
    }

    @Test
    @DisplayName("낙관적 락 충돌이 2번 발생하고 3번째에 성공하면, process()는 예외 없이 총 3번 호출된다")
    void shouldSucceedAndCallThreeTimes_whenOptimisticLockingFailureOccursTwiceAndSucceedsOnThird() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .doThrow(new OptimisticLockingFailureException("version conflict"))
                .doNothing()
                .when(orderCouponService).process(any());

        //when, then
        assertThatCode(() -> orderCouponSagaCoordinator.process(couponResponse))
                .doesNotThrowAnyException();

        verify(orderCouponService, times(3)).process(any());
    }

    @Test
    @DisplayName("maxAttempts(3)를 초과해 계속 충돌하면 결국 예외가 호출부로 전파된다")
    void shouldPropagateException_whenRetriesExceedMaxAttempts() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .when(orderCouponService).process(any());

        //when, then
        assertThatThrownBy(() -> orderCouponSagaCoordinator.process(couponResponse))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(orderCouponService, times(3)).process(any());
    }

    @Test
    @DisplayName("첫 시도에 바로 성공하면 재시도 없이 1번만 호출된다")
    void shouldCallOnlyOnce_whenSucceedsOnFirstAttempt() {
        //given
        doNothing().when(orderCouponService).process(any());

        //when
        orderCouponSagaCoordinator.process(couponResponse);

        //then
        verify(orderCouponService, times(1)).process(any());
    }

    @Test
    @DisplayName("rollback()도 동일하게 재시도가 적용된다")
    void shouldRetry_whenOptimisticLockingFailureOccursOnRollback() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .doNothing()
                .when(orderCouponService).rollback(any());

        //when, then
        assertThatCode(() -> orderCouponSagaCoordinator.rollback(couponResponse))
                .doesNotThrowAnyException();

        verify(orderCouponService, times(2)).rollback(any());
    }

}
