package com.orderingsystem.payment.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.mapper.PaymentDataMapper;
import com.orderingsystem.payment.application.outbox.OrderOutboxHelper;
import com.orderingsystem.payment.domain.event.PaymentEvent;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.CreditInfo;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.outbox.MessageType;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.payment.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.payment.domain.service.PaymentValidateAndCancelService;
import com.orderingsystem.payment.domain.service.PaymentValidateAndInitiateService;
import java.time.ZonedDateTime;
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

    private final PaymentRepository paymentRepository;
    private final PaymentValidateAndInitiateService paymentValidateAndInitiateService;
    private final CreditEntryRepository creditEntryRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final PaymentDataMapper paymentDataMapper;
    private final PaymentValidateAndCancelService paymentValidateAndCancelService;
    private final ProcessedMessageRepository processedMessageRepository;
    private final OrderOutboxRepository orderOutboxRepository;

    @Transactional
    public void completePayment(PaymentRequest paymentRequest) {
        if (checkAndMarkProcessed(paymentRequest, MessageType.COMPLETE_PAYMENT) &&
                isOutboxMessageProcessedForPayment(paymentRequest, PaymentStatus.COMPLETED)) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 결제 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    paymentRequest.getSagaId());
            return;
        }

        log.info("결제 이벤트 수신. Order Id : {}", paymentRequest.getOrderId());

        Payment payment = paymentRequest.toPayment();

        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
        List<String> failureMessages = new ArrayList<>();
        CreditInfo creditInfo = CreditInfo.builder()
                .id(creditEntry.getId())
                .customerId(creditEntry.getCustomerId())
                .totalCreditAmount(creditEntry.getTotalCreditAmount())
                .build();

        PaymentEvent paymentEvent = paymentValidateAndInitiateService.validateAndInitiate(payment, creditInfo,
                creditHistories, failureMessages, paymentRequest);

        persistCompleteDataBase(payment, creditEntry, creditInfo, creditHistories, failureMessages, payment.getPrice());

        orderOutboxHelper.saveOrderOutboxMessage(
                paymentDataMapper.paymentEventToOrderEventPayload(paymentEvent, paymentRequest.getSagaId(),
                        paymentRequest.getFailureMessages()),
                paymentEvent.getPayment().getStatus(),
                paymentRequest.getSagaId());
    }

    @Transactional
    public void cancelPayment(PaymentRequest paymentRequest, OrderStatus orderStatus) {
        if (checkAndMarkProcessed(paymentRequest, MessageType.CANCEL_PAYMENT) &&
                isOutboxMessageProcessedForPayment(paymentRequest, PaymentStatus.CANCELLED)) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 취소 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    paymentRequest.getSagaId());
            return;
        }

        log.info("결제 rollback 이벤트를 받았습니다. Order ID : {}", paymentRequest.getOrderId());

        Payment payment = getPayment(paymentRequest.getOrderId());
        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistories(payment.getCustomerId());
        List<String> failureMessages = new ArrayList<>();

        PaymentEvent paymentEvent = null;

        if (orderStatus.equals(OrderStatus.CANCELLING)) {
            paymentEvent = paymentValidateAndCancelService.validateAndCancel(payment, creditEntry,
                    creditHistories, failureMessages);


        } else if (orderStatus.equals(OrderStatus.REJECTING)) {
            paymentEvent = paymentValidateAndCancelService.validateAndRefund(payment, creditEntry,
                    creditHistories, failureMessages);
        }

        if (failureMessages.isEmpty()) {
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }

        persistCancelDataBase(payment, creditHistories, failureMessages);

        orderOutboxHelper.saveOrderOutboxMessage(
                paymentDataMapper.paymentEventToOrderEventPayload(paymentEvent, paymentRequest.getSagaId(),
                        paymentRequest.getFailureMessages()),
                paymentEvent.getPayment().getStatus(),
                paymentRequest.getSagaId());
    }

    private boolean checkAndMarkProcessed(PaymentRequest paymentRequest, MessageType messageType) {
        int inserted = processedMessageRepository.insertIgnore(
                paymentRequest.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );

        return inserted == 0;
    }

    private boolean isOutboxMessageProcessedForPayment(PaymentRequest paymentRequest,
                                                       PaymentStatus paymentStatus) {
        Optional<OrderOutbox> orderOutboxMessage = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatus(
                ORDER_SAGA_NAME, paymentRequest.getSagaId(), paymentStatus);

        return orderOutboxMessage.isPresent();
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
        List<CreditHistory> creditHistories = creditHistoryRepository.findByCustomerId(customerId);

        if (creditHistories.isEmpty()) {
            log.error("해당 고객에 대한 CreditHistory 정보를 찾을 수 없습니다. Customer Id : {}", customerId);
            throw new PaymentApplicationException("해당 고객에 대한 CreditHistory 정보를 찾을 수 없습니다. Customer Id : " + customerId);
        }

        return creditHistories;
    }

    private void persistCompleteDataBase(Payment payment, CreditEntry creditEntry, CreditInfo creditInfo,
                                         List<CreditHistory> creditHistories,
                                         List<String> failureMassages, Money price) {
        paymentRepository.save(payment);
        if (failureMassages.isEmpty()) {
            if (creditInfo.getTotalCreditAmount().equals(creditEntry.getTotalCreditAmount().subtract(price))) {
                creditEntry.subtractCreditAmount(price);
                creditEntryRepository.save(creditEntry);
            }
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }
    }

    private void persistCancelDataBase(Payment payment, List<CreditHistory> creditHistories,
                                       List<String> failureMassages) {
        paymentRepository.save(payment);
        if (failureMassages.isEmpty()) {
            creditHistoryRepository.save(creditHistories.get(creditHistories.size() - 1));
        }
    }
}
