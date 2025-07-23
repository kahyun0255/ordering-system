package com.orderingsystem.payment.domain.event;

import com.orderingsystem.payment.domain.model.Payment;
import java.time.ZonedDateTime;
import java.util.List;

public class PaymentFailedEvent extends PaymentEvent {

    public PaymentFailedEvent(Payment payment, ZonedDateTime createdAt, List<String> failureMessages) {
        super(payment, createdAt, failureMessages);
    }

    @Override
    public void fire() {

    }
}
