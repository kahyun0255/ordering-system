package com.orderingsystem.payment.application.outbox;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.exception.PaymentDomainException;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import java.time.ZonedDateTime;
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
public class OrderOutboxHelper {
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void updateOutboxMessage(OrderOutbox orderOutbox, OutboxStatus outboxStatus){
        orderOutbox.updateOutboxStatus(outboxStatus);
        log.info("Payment OrderOutbox Status 업데이트 : {}", outboxStatus);
    }

    @Transactional
    public void save(OrderOutbox orderOutbox){
        try {
            orderOutboxRepository.save(orderOutbox);
            log.info("Payment OrderOutbox 저장했습니다. Outbox Id : {}", orderOutbox.getId());
        } catch (Exception e) {
            log.error("payment OrderOutbox 저장에 실패했습니다. Outbox Id : {}, Message : {}",
                    orderOutbox.getId(), e.getMessage());
            throw new PaymentDomainException(
                    "payment OrderOutbox 저장에 실패했습니다. Outbox Id : " + orderOutbox.getId() +
                            " Message : " + e.getMessage());
        }
    }

    @Transactional
    public void saveOrderOutboxMessage(OrderEventPayload orderEventPayload, PaymentStatus paymentStatus,
                                       OutboxStatus outboxStatus, UUID sagaId) {
        save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .processedAt(ZonedDateTime.now())
                .createdAt(orderEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(orderEventPayload))
                .outboxStatus(outboxStatus)
                .paymentStatus(paymentStatus)
                .build());
    }

    private String createPayload(OrderEventPayload orderEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderEventPayload);
        } catch (JsonProcessingException e) {
            log.error("OrderEventPayload 생성에 실패했습니다. Order Id : {}", orderEventPayload.getOrderId());
            throw new PaymentDomainException(
                    "OrderEventPayload 생성에 실패했습니다. Order Id : " + orderEventPayload.getOrderId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<OrderOutbox>> getOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        return orderOutboxRepository.findByTypeAndOutboxStatus(ORDER_SAGA_NAME, outboxStatus);
    }

    @Transactional
    public void deleteOrderOutboxByOutboxStatus(OutboxStatus outboxStatus) {
        orderOutboxRepository.deleteAllByTypeAndOutboxStatus(ORDER_SAGA_NAME, outboxStatus);
    }
}
