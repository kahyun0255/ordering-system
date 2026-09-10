package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.order.application.dto.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import jakarta.annotation.Resource;

@SpringJUnitConfig(OrderPaymentSagaCoordinatorRetryTest.RetryTestConfig.class)
class OrderPaymentSagaCoordinatorRetryTest {

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    static class RetryTestConfig {

        @Bean
        OrderPaymentService orderPaymentService() {
            return mock(OrderPaymentService.class);
        }

        @Bean
        OrderPaymentSagaCoordinator orderPaymentSagaCoordinator(OrderPaymentService orderPaymentService) {
            return new OrderPaymentSagaCoordinator(orderPaymentService);
        }
    }

    @Resource
    private OrderPaymentService orderPaymentService;

    @Resource
    private OrderPaymentSagaCoordinator orderPaymentSagaCoordinator;

    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        paymentResponse = PaymentResponse.builder().build();
        org.mockito.Mockito.reset(orderPaymentService);
    }

    @Test
    @DisplayName("낙관적 락 충돌이 2번 발생하고 3번째에 성공하면, process()는 예외 없이 총 3번 호출된다")
    void shouldSucceedAndCallThreeTimes_whenOptimisticLockingFailureOccursTwiceAndSucceedsOnThird() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .doThrow(new OptimisticLockingFailureException("version conflict"))
                .doNothing()
                .when(orderPaymentService).process(any());

        //when, then
        assertThatCode(() -> orderPaymentSagaCoordinator.process(paymentResponse))
                .doesNotThrowAnyException();

        verify(orderPaymentService, times(3)).process(any());
    }

    @Test
    @DisplayName("maxAttempts(3)를 초과해 계속 충돌하면 결국 예외가 호출부로 전파된다")
    void shouldPropagateException_whenRetriesExceedMaxAttempts() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .when(orderPaymentService).process(any());

        //when, then
        assertThatThrownBy(() -> orderPaymentSagaCoordinator.process(paymentResponse))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(orderPaymentService, times(3)).process(any());
    }

    @Test
    @DisplayName("첫 시도에 바로 성공하면 재시도 없이 1번만 호출된다")
    void shouldCallOnlyOnce_whenSucceedsOnFirstAttempt() {
        //given
        doNothing().when(orderPaymentService).process(any());

        //when
        orderPaymentSagaCoordinator.process(paymentResponse);

        //then
        verify(orderPaymentService, times(1)).process(any());
    }

    @Test
    @DisplayName("rollback()도 동일하게 재시도가 적용된다")
    void shouldRetry_whenOptimisticLockingFailureOccursOnRollback() {
        //given
        doThrow(new OptimisticLockingFailureException("version conflict"))
                .doNothing()
                .when(orderPaymentService).rollback(any());

        //when, then
        assertThatCode(() -> orderPaymentSagaCoordinator.rollback(paymentResponse))
                .doesNotThrowAnyException();

        verify(orderPaymentService, times(2)).rollback(any());
    }

}
