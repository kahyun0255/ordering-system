package com.orderingsystem.payment.domain.service;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.domain.event.PaymentCancelledEvent;

import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.event.PaymentFailedEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.CreditInfo;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentValidateAndCancelService {

    public PaymentEvent validateAndCancel(Payment payment, CreditEntry creditEntry,
                                          List<CreditHistory> creditHistories, List<String> failureMessages,
                                          DomainEventPublisher<PaymentCancelledEvent> paymentCancelledEventDomainEventPublisher,
                                          DomainEventPublisher<PaymentFailedEvent> paymentFailedEventDomainEventPublisher) {
        payment.validatePayment(failureMessages);
        addCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistories, TransactionType.CREDIT);

        if (failureMessages.isEmpty()) {
            payment.updateStatus(PaymentStatus.CANCELLED);
            log.info("결제가 취소되었습니다. Order Id : {}", payment.getOrderId());
            return new PaymentCancelledEvent(payment, ZonedDateTime.now(), paymentCancelledEventDomainEventPublisher);
        } else {
            payment.updateStatus(PaymentStatus.FAILED);
            log.error("결제 취소에 실패했습니다. Order Id : {}", payment.getOrderId());
            return new PaymentFailedEvent(payment, ZonedDateTime.now(), failureMessages,
                    paymentFailedEventDomainEventPublisher);
        }
    }

    private void addCreditEntry(Payment payment, CreditEntry creditEntry) {
        creditEntry.addCreditAmount(payment.getPrice());
    }

    private void updateCreditHistory(Payment payment, List<CreditHistory> creditHistories,
                                     TransactionType transactionType) {
        creditHistories.add(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(payment.getCustomerId())
                .amount(payment.getPrice())
                .type(transactionType)
                .build());
    }
}
