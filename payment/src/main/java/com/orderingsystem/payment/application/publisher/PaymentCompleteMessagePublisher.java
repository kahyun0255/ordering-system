package com.orderingsystem.payment.application.publisher;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.payment.domain.event.PaymentCompletedEvent;

public interface PaymentCompleteMessagePublisher extends DomainEventPublisher<PaymentCompletedEvent> {
}
