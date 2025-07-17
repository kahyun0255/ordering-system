package com.orderingsystem.payment.application;

import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.publisher.PaymentCompleteMessagePublisher;
import com.orderingsystem.payment.application.publisher.PaymentFailedMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndInitiateService;
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
    private final PaymentValidateAndInitiateService paymentValidateAndInitiateService;
    private final PaymentCompleteMessagePublisher paymentCompleteMessagePublisher;
    private final PaymentFailedMessagePublisher paymentFailedMessagePublisher;
    private final PaymentRepository paymentRepository;


    @Transactional
    public PaymentEvent persistPayment(PaymentRequest paymentRequest) {
        log.info("결제 완료 이벤트 수신. Order Id : {}", paymentRequest.getOrderId());

        Payment payment = paymentRequest.toPayment();

        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
        List<String> failureMassages = new ArrayList<>();

        PaymentEvent paymentEvent = paymentValidateAndInitiateService.validateAndInitiate(payment, creditEntry,
                creditHistories, failureMassages,
                paymentCompleteMessagePublisher, paymentFailedMessagePublisher);

        persistDataBase(payment, creditEntry, creditHistories, failureMassages);

        return paymentEvent;
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
                                 List<String> failureMassages) {
        paymentRepository.save(payment);
        if (failureMassages.isEmpty()) {
            creditEntryRepository.save(creditEntry);
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }
    }
}
