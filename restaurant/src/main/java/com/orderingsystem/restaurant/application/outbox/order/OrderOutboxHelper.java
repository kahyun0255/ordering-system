package com.orderingsystem.restaurant.application.outbox.order;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.order.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.exception.RestaurantDomainException;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
import java.time.ZonedDateTime;
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
public class OrderOutboxHelper {

    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(OrderOutbox orderOutbox) {
        if (!orderOutboxRepository.existsByTypeAndSagaIdAndOrderApprovalStatusAndOutboxStatus(ORDER_SAGA_NAME,
                orderOutbox.getSagaId(), orderOutbox.getOrderApprovalStatus(), orderOutbox.getOutboxStatus())) {
            orderOutboxRepository.save(orderOutbox);
            log.info("Restaurant OrderOutbox 저장했습니다. Outbox Id : {}", orderOutbox.getId());
        } else {
            log.warn("이미 저장된 Restaurant OrderOutbox가 존재합니다. Saga Id : {}, Type : {}, ApprovalStatus : {}",
                    orderOutbox.getSagaId(), orderOutbox.getType(), orderOutbox.getOrderApprovalStatus());
        }
    }

    @Transactional
    public void saveOrderOutboxMessage(OrderEventPayload orderEventPayload, OrderApprovalStatus status,
                                       OutboxStatus outboxStatus, UUID sagaId) {
        save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(orderEventPayload.getCreatedAt())
                .processedAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(orderEventPayload))
                .outboxStatus(outboxStatus)
                .orderApprovalStatus(status)
                .build());
    }

    private String createPayload(OrderEventPayload orderEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderEventPayload);
        } catch (JsonProcessingException e) {
            log.error("OrderEventPayload 생성에 실패했습니다. Order Id : {}", orderEventPayload.getOrderId());
            throw new RestaurantDomainException(
                    "OrderEventPayload 생성에 실패했습니다. Order Id : " + orderEventPayload.getOrderId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<OrderOutbox>> getOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        return orderOutboxRepository.findByTypeAndOutboxStatus(ORDER_SAGA_NAME, outboxStatus);
    }

    @Transactional
    public void deleteAllOrderOutboxByOutboxStatus(OutboxStatus outboxStatus) {
        orderOutboxRepository.deleteAllByTypeAndOutboxStatus(ORDER_SAGA_NAME, outboxStatus);
    }
}
