package com.orderingsystem.order.application;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusToSagaStatus {

    static SagaStatus orderStatusToSagaStatus(OrderStatus orderStatus){
        return switch (orderStatus) {
            case PAID -> SagaStatus.PROCESSING;
            case APPROVED -> SagaStatus.SUCCEEDED;
            case CANCELLING -> SagaStatus.COMPENSATING;
            case CANCELLED -> SagaStatus.COMPENSATED;
            default -> SagaStatus.STARTED;
        };
    }

}
