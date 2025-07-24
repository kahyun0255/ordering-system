package com.orderingsystem.order.application.outbox.restaurant;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.outbox.RestaurantApprovalOutboxRepository;
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
@Slf4j
@RequiredArgsConstructor
public class RestaurantApprovalOutboxHelper {

    private final RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(RestaurantApprovalOutbox restaurantApprovalOutbox) {
        try {
            restaurantApprovalOutboxRepository.save(restaurantApprovalOutbox);
            log.info("Order RestaurantApprovalOutbox 저장했습니다. Outbox Id : {}", restaurantApprovalOutbox.getId());
        } catch (Exception e) {
            log.error("Order RestaurantApprovalOutbox 저장에 실패했습니다. Outbox Id : {}, Message : {}",
                    restaurantApprovalOutbox.getId(), e.getMessage());
            throw new OrderDomainException(
                    "Order RestaurantApprovalOutbox 저장에 실패했습니다. Outbox Id : " + restaurantApprovalOutbox.getId() +
                            " Message : " + e.getMessage());
        }
    }

    @Transactional
    public void saveRestaurantApprovalOutboxMessage(RestaurantApprovalEventPayload restaurantApprovalEventPayload,
                                                    OrderStatus orderStatus, SagaStatus sagaStatus,
                                                    OutboxStatus outboxStatus,
                                                    UUID sagaId) {
        save(RestaurantApprovalOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(restaurantApprovalEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(restaurantApprovalEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .outboxStatus(outboxStatus)
                .build());
    }

    private String createPayload(RestaurantApprovalEventPayload restaurantApprovalEventPayload) {
        try {
            return objectMapper.writeValueAsString(restaurantApprovalEventPayload);
        } catch (JsonProcessingException e) {
            log.error("RestaurantApprovalEventPayload 생성에 실패했습니다. Order Id : {}",
                    restaurantApprovalEventPayload.getOrderId());
            throw new OrderDomainException("RestaurantApprovalEventPayload 생성에 실패했습니다. Order Id : "
                    + restaurantApprovalEventPayload.getOrderId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<RestaurantApprovalOutbox>> getApprovalOutboxMessagesByOutboxStatusAndSagaStatus(
            OutboxStatus outboxStatus, SagaStatus sagaStatus) {
        return restaurantApprovalOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, outboxStatus,
                sagaStatus);
    }

    public Optional<List<RestaurantApprovalOutbox>> getRestaurantApprovalOutboxMessagesByOutboxStatusAndOutboxSagaStatus(
            OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        return restaurantApprovalOutboxRepository.findByTypeAndOutboxStatusAndSagaStatusIn(
                ORDER_SAGA_NAME, outboxStatus,
                Arrays.asList(sagaStatus));
    }

    public void deleteAllRestaurantApprovalOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                                    SagaStatus... sagaStatus) {
        restaurantApprovalOutboxRepository.deleteAllByTypeAndOutboxStatusAndSagaStatusIn(
                ORDER_SAGA_NAME, outboxStatus, Arrays.asList(sagaStatus));
    }
}
