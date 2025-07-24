package com.orderingsystem.payment.domain.event;

import com.orderingsystem.payment.domain.model.Payment;
import java.time.ZonedDateTime;
import java.util.Collections;

public class PaymentCancelledEvent extends PaymentEvent {

    public PaymentCancelledEvent(Payment payment, ZonedDateTime createdAt) {
        super(payment, createdAt, Collections.emptyList());
    }

}
