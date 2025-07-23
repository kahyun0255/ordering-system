package com.orderingsystem.order.application.publisher;

import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.function.BiConsumer;

public interface PaymentRequestMessagePublisher {

    void publisher(PaymentOutbox paymentOutbox, BiConsumer<PaymentOutbox, OutboxStatus> outboxCallBack);

}
