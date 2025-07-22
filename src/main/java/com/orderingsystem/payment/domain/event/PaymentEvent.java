package com.orderingsystem.payment.domain.event;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.payment.domain.model.Payment;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class PaymentEvent implements DomainEvent<Payment> {

    private final Payment payment;
    private final ZonedDateTime createdAt;
    private final List<String> failureMessages;

}
