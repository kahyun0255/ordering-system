package com.orderingsystem.payment.domain.event;

import com.orderingsystem.payment.domain.model.Payment;
import java.time.ZonedDateTime;
import java.util.Collections;

public class PaymentCompletedEvent extends PaymentEvent {

    public PaymentCompletedEvent(Payment payment, ZonedDateTime createdAt) {
        super(payment, createdAt, Collections.emptyList());
    }

    @Override
    public void fire() {

    }
}
