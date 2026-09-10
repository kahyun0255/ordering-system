package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentSagaCoordinator implements SagaStep<PaymentResponse> {

    private final OrderPaymentService orderPaymentService;

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void process(PaymentResponse paymentResponse) {
        orderPaymentService.process(paymentResponse);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void rollback(PaymentResponse paymentResponse) {
        orderPaymentService.rollback(paymentResponse);
    }

}
