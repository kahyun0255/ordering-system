package com.orderingsystem.order.application.outbox.payment;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxHelper {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(PaymentOutbox paymentOutbox) {
        try {
            paymentOutboxRepository.save(paymentOutbox);
            log.info("Order PaymentOutbox 저장했습니다. Outbox Id : {}", paymentOutbox.getId());
        } catch (Exception e) {
            log.error("Order PaymentOutbox 저장에 실패했습니다. Outbox Id : {}, Message : {}",
                    paymentOutbox.getId(), e.getMessage());
            throw new OrderDomainException(
                    "Order PaymentOutbox 저장에 실패했습니다. Outbox Id : " + paymentOutbox.getId() +
                            " Message : " + e.getMessage());
        }
    }

    @Transactional
    public void savePaymentOutboxMessage(OrderPaymentEventPayload orderPaymentEventPayload, OrderStatus orderStatus,
                                         SagaStatus sagaStatus, OutboxStatus outboxStatus, UUID sagaId) {
        save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createAt(orderPaymentEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(orderPaymentEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .outboxStatus(outboxStatus)
                .build());
    }

    private String createPayload(OrderPaymentEventPayload orderPaymentEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderPaymentEventPayload);
        } catch (JsonProcessingException e) {
            log.error("OrderPaymentEventPayload 생성에 실패했습니다. Order Id : {}", orderPaymentEventPayload.getOrderId());
            throw new OrderDomainException(
                    "OrderPaymentEventPayload 생성에 실패했습니다. Order Id : " + orderPaymentEventPayload.getOrderId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<PaymentOutbox>> getPaymentOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
            OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        return paymentOutboxRepository.findByTypeAndOutboxStatusAndSagaStatusIn(
                ORDER_SAGA_NAME, outboxStatus,
                Arrays.asList(sagaStatus));
    }

    @Transactional
    public void deleteAllPaymentOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                         SagaStatus... sagaStatus) {
        paymentOutboxRepository.deleteAllByTypeAndOutboxStatusAndSagaStatusIn(
                ORDER_SAGA_NAME, outboxStatus, Arrays.asList(sagaStatus));
    }

    @Transactional
    public Optional<PaymentOutbox> getPaymentOutboxBySagaIdAndSagaStatus(UUID sagaId, SagaStatus... sagaStatus) {
        return paymentOutboxRepository.findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId, Arrays.asList(sagaStatus));
    }
}
