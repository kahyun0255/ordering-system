package com.orderingsystem.payment.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.mapper.PaymentDataMapper;
import com.orderingsystem.payment.application.outbox.OrderOutboxHelper;
import com.orderingsystem.payment.application.publisher.PaymentResponseMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndInitiateService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRequestHelper paymentRequestHelper;
    private final PaymentRepository paymentRepository;
    private final PaymentValidateAndInitiateService paymentValidateAndInitiateService;
    private final CreditEntryRepository creditEntryRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final PaymentResponseMessagePublisher paymentResponseMessagePublisher;
    private final OrderOutboxHelper orderOutboxHelper;
    private final PaymentDataMapper paymentDataMapper;

    @Transactional
    public void completePayment(PaymentRequest paymentRequest) {
        if (publishIfOutboxMessageProcessedForPayment(paymentRequest, PaymentStatus.COMPLETED)) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 처리 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    paymentRequest.getSagaId());
            return;
        }

        log.info("결제 이벤트 수신. Order Id : {}", paymentRequest.getOrderId());

        Payment payment = paymentRequest.toPayment();

        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
        List<String> failureMassages = new ArrayList<>();

        PaymentEvent paymentEvent = paymentValidateAndInitiateService.validateAndInitiate(payment, creditEntry,
                creditHistories, failureMassages);

        persistDataBase(payment, creditEntry, creditHistories, failureMassages, payment.getPrice());

        orderOutboxHelper.saveOrderOutboxMessage(
                paymentDataMapper.paymentEventToOrderEventPayload(paymentEvent),
                paymentEvent.getPayment().getStatus(),
                OutboxStatus.STARTED,
                paymentRequest.getSagaId());
    }

    public void cancelPayment(PaymentRequest paymentRequest) {
        PaymentEvent paymentEvent = paymentRequestHelper.persistCancelPayment(paymentRequest);

        log.info("결제 이벤트 발행. Payment Id : {}, Order Id : {}", paymentEvent.getPayment().getId(),
                paymentEvent.getPayment().getOrderId());
        paymentEvent.fire();
    }

    private boolean publishIfOutboxMessageProcessedForPayment(PaymentRequest paymentRequest,
                                                              PaymentStatus paymentStatus) {
        Optional<OrderOutbox> orderOutboxMessage = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, paymentRequest.getSagaId(), paymentStatus, OutboxStatus.COMPLETED);

        if (orderOutboxMessage.isPresent()){
            paymentResponseMessagePublisher.publish(orderOutboxMessage.get(), orderOutboxHelper::updateOutboxMessage);
            return true;
        }
        return false;
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
