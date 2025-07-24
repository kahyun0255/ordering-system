package com.orderingsystem.payment.application.publisher;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import java.util.function.BiConsumer;

public interface PaymentResponseMessagePublisher {
    void publish(OrderOutbox orderOutbox, BiConsumer<OrderOutbox, OutboxStatus> outboxCallback);
}
