package com.orderingsystem.payment.domain.service;

import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.CreditInfo;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentValidateAndInitiateService {

    private final ValidateCreditHistoryService validateCreditHistoryService;

    public PaymentEvent validateAndInitiate(Payment payment, CreditInfo creditInfo,
                                            List<CreditHistory> creditHistories,
                                            List<String> failureMessages, PaymentRequest paymentRequest) {

        payment.validatePayment(failureMessages);
        payment.initializePayment();

        validateCreditEntry(payment, creditInfo, failureMessages);
        subtractCreditEntry(payment, creditInfo);
        updateCreditHistory(payment, creditHistories, paymentRequest);
        validateCreditHistoryService.validateCreditHistory(creditInfo, creditHistories, failureMessages);

        if (failureMessages.isEmpty()) {
            log.info("Order Id에 대한 결제가 준비되었습니다. Order Id : {}", payment.getOrderId());
            return payment.complete();
        } else {
            log.info("Order Id에 대한 결제 준비가 실패했습니다. Order Id : {}", payment.getOrderId());
            return payment.fail(failureMessages);
        }
    }

    private void validateCreditEntry(Payment payment, CreditInfo creditInfo, List<String> failureMessages) {
        if (payment.getPrice().isGreaterThan(creditInfo.getTotalCreditAmount())) {
            log.error("고객의 크레딧이 결제 금액보다 부족합니다. Customer Id : {}", payment.getCustomerId());
            failureMessages.add("고객의 크레딧이 결제 금액보다 부족합니다.");
        }
    }

    private void subtractCreditEntry(Payment payment, CreditInfo creditInfo) {
        creditInfo.subtractCreditAmount(payment.getPrice());
    }

    private void updateCreditHistory(Payment payment, List<CreditHistory> creditHistories,
                                     PaymentRequest paymentRequest) {
        creditHistories.add(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(payment.getCustomerId())
                .amount(payment.getPrice())
                .type(TransactionType.DEBIT)
                .orderId(paymentRequest.getOrderId())
                .build());
    }

}
