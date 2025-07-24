package com.orderingsystem.restaurant.application.outbox;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.model.OrderEventPayload;
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
        try {
            orderOutboxRepository.save(orderOutbox);
            log.info("Restaurant OrderOutbox 저장했습니다. Outbox Id : {}", orderOutbox.getId());
        } catch (Exception e) {
            log.error("Restaurant OrderOutbox 저장에 실패했습니다. Outbox Id : {}, Message : {}",
                    orderOutbox.getId(), e.getMessage());
            throw new RestaurantDomainException(
                    "Restaurant OrderOutbox 저장에 실패했습니다. Outbox Id : " + orderOutbox.getId() +
                            " Message : " + e.getMessage());
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
    public void deleteOrderOutboxByOutboxStatus(OutboxStatus outboxStatus) {
        orderOutboxRepository.deleteByTypeAndOutboxStatus(ORDER_SAGA_NAME, outboxStatus);
    }
}
