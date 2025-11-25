package com.orderingsystem.payment.domain.service;

import com.orderingsystem.payment.domain.event.PaymentCancelledEvent;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.event.PaymentRefundedEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class PaymentValidateAndCancelService {

    @Transactional
    public PaymentEvent validateAndCancel(Payment payment, CreditEntry creditEntry,
                                          List<CreditHistory> creditHistories, List<String> failureMessages) {
        payment.validatePayment(failureMessages);

        if (!failureMessages.isEmpty()) {
            log.error("결제 취소에 실패했습니다. Order Id : {}", payment.getOrderId());
            return payment.fail(failureMessages);
        }

        addCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistories);

        PaymentCancelledEvent paymentCancelledEvent = payment.cancel();
        log.info("결제가 취소되었습니다. Order Id : {}", payment.getOrderId());
        return paymentCancelledEvent;
    }

    public PaymentEvent validateAndRefund(Payment payment, CreditEntry creditEntry, List<CreditHistory> creditHistories,
                                          List<String> failureMessages) {
        payment.validatePayment(failureMessages);

        if (!failureMessages.isEmpty()) {
            log.error("결제 취소에 실패했습니다. Order Id : {}", payment.getOrderId());
            return payment.fail(failureMessages);
        }

        addCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistories);

        PaymentRefundedEvent paymentRefundedEvent = payment.refund();
        log.info("결제가 환불되었습니다. Order Id : {}", payment.getOrderId());
        return paymentRefundedEvent;
    }

    private void addCreditEntry(Payment payment, CreditEntry creditEntry) {
        creditEntry.addCreditAmount(payment.getPrice());
    }

    private void updateCreditHistory(Payment payment, List<CreditHistory> creditHistories) {
        creditHistories.add(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(payment.getCustomerId())
                .orderId(payment.getOrderId())
                .amount(payment.getPrice())
                .type(TransactionType.CREDIT)
                .paidAt(ZonedDateTime.now())
                .build());
    }
}
