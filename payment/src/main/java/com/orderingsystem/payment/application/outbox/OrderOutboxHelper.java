package com.orderingsystem.payment.application.outbox;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentStatus;
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
    public void save(OrderOutbox orderOutbox) {
        if (!orderOutboxRepository.existsByTypeAndSagaIdAndPaymentStatus(ORDER_SAGA_NAME,
                orderOutbox.getSagaId(), orderOutbox.getPaymentStatus())) {
            orderOutboxRepository.save(orderOutbox);
            log.info("Payment OrderOutbox 저장했습니다. Outbox Id : {}", orderOutbox.getId());
        } else {
            log.warn("이미 저장된 Payment OrderOutbox가 존재합니다. Saga Id : {}, Type : {}, PaymentStatus : {}",
                    orderOutbox.getSagaId(), orderOutbox.getType(), orderOutbox.getPaymentStatus());
        }
    }

    @Transactional
    public void saveOrderOutboxMessage(OrderEventPayload orderEventPayload, PaymentStatus paymentStatus, UUID sagaId) {
        save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .processedAt(ZonedDateTime.now())
                .createdAt(orderEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(orderEventPayload))
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
    public Optional<List<OrderOutbox>> getOrderOutboxMessage() {
        return orderOutboxRepository.findByType(ORDER_SAGA_NAME);
    }

    @Transactional
    public void deleteAllOrderOutbox() {
        orderOutboxRepository.deleteAllByType(ORDER_SAGA_NAME);
    }
}
