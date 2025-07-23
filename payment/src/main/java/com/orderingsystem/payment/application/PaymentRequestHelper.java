package com.orderingsystem.payment.application;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.publisher.PaymentCancelledMessagePublisher;
import com.orderingsystem.payment.application.publisher.PaymentFailedMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndCancelService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentRequestHelper {

    private final CreditEntryRepository creditEntryRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final PaymentFailedMessagePublisher paymentFailedMessagePublisher;
    private final PaymentRepository paymentRepository;
    private final PaymentValidateAndCancelService paymentValidateAndCancelService;
    private final PaymentCancelledMessagePublisher paymentCancelledMessagePublisher;

    @Transactional
    public PaymentEvent persistCancelPayment(PaymentRequest paymentRequest) {
        log.info("결제 rollback 이벤트를 받았습니다. Order ID : {}", paymentRequest.getOrderId());

        Payment payment = getPayment(paymentRequest.getOrderId());
        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
        List<String> failureMessages = new ArrayList<>();

        PaymentEvent paymentEvent = paymentValidateAndCancelService.validateAndCancel(payment, creditEntry,
                creditHistories,
                failureMessages, paymentCancelledMessagePublisher, paymentFailedMessagePublisher);

        if (failureMessages.isEmpty()) {
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }

        return paymentEvent;
    }

    private Payment getPayment(UUID orderId) {
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);

        if (payment.isEmpty()) {
            log.error("해당 주문에 대한 결제 정보를 찾지 못했습니다. Order Id : {}", orderId);
            throw new PaymentApplicationException("해당 주문에 대한 결제 정보를 찾지 못했습니다. Order Id : " + orderId);
        }

        return payment.get();
    }

    private CreditEntry getCreditEntry(UUID customerId) {
        Optional<CreditEntry> creditEntry = creditEntryRepository.findByCustomerId(customerId);

        if (creditEntry.isEmpty()) {
            log.error("해당 고객에 대한 CreditEntry 정보를 찾을 수 없습니다. Customer Id : {}", customerId);
            throw new PaymentApplicationException("해당 고객에 대한 CreditEntry 정보를 찾을 수 없습니다. Customer Id : " + customerId);
        }

        return creditEntry.get();
    }

    private List<CreditHistory> getCreditHistories(UUID customerId) {
        Optional<List<CreditHistory>> creditHistories = creditHistoryRepository.findByCustomerId(customerId);

        if (creditHistories.isEmpty()) {
            log.error("해당 고객에 대한 CreditHistory 정보를 찾을 수 없습니다. Customer Id : {}", customerId);
            throw new PaymentApplicationException("해당 고객에 대한 CreditHistory 정보를 찾을 수 없습니다. Customer Id : " + customerId);
        }

        return creditHistories.get();
    }

    private void persistDataBase(Payment payment, CreditEntry creditEntry, List<CreditHistory> creditHistories,
                                 List<String> failureMassages, Money price) {
        paymentRepository.save(payment);
        if (failureMassages.isEmpty()) {
            creditEntry.subtractCreditAmount(price);
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }
    }
}
