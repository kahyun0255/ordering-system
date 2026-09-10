package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCouponSagaCoordinator implements SagaStep<CouponResponse> {

    private final OrderCouponService orderCouponService;

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void process(CouponResponse couponResponse) {
        orderCouponService.process(couponResponse);
    }

    @Override
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void rollback(CouponResponse couponResponse) {
        orderCouponService.rollback(couponResponse);
    }

}
