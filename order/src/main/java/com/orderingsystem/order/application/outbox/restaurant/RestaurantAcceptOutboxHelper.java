package com.orderingsystem.order.application.outbox.restaurant;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantAcceptEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import com.orderingsystem.order.domain.repository.outbox.RestaurantAcceptOutboxRepository;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantAcceptOutboxHelper {

    private final RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(RestaurantAcceptOutbox restaurantAcceptOutbox) {
        if (!restaurantAcceptOutboxRepository.existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(ORDER_SAGA_NAME,
                restaurantAcceptOutbox.getSagaId(), restaurantAcceptOutbox.getOrderStatus(),
                restaurantAcceptOutbox.getSagaStatus())) {
            restaurantAcceptOutboxRepository.save(restaurantAcceptOutbox);
            log.info("Order RestaurantAcceptOutbox 저장했습니다. Outbox Id : {}", restaurantAcceptOutbox.getId());
        } else {
            log.warn("이미 저장된 Order RestaurantAcceptOutbox 존재합니다. sagaId : {}, type : {}, orderStatus : {}",
                    restaurantAcceptOutbox.getSagaStatus(), restaurantAcceptOutbox.getType(),
                    restaurantAcceptOutbox.getOrderStatus());
        }
    }

    @Transactional
    public void saveRestaurantAcceptOutboxMessage(RestaurantAcceptEventPayload restaurantAcceptEventPayload,
                                                  OrderStatus orderStatus, SagaStatus sagaStatus,
                                                  UUID sagaId) {
        save(RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(restaurantAcceptEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(restaurantAcceptEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .build());
    }

    private String createPayload(RestaurantAcceptEventPayload restaurantAcceptEventPayload) {
        try {
            return objectMapper.writeValueAsString(restaurantAcceptEventPayload);
        } catch (JsonProcessingException e) {
            log.error("RestaurantAcceptEventPayload 생성에 실패했습니다. Order Id : {}",
                    restaurantAcceptEventPayload.getOrderId());
            throw new OrderDomainException("RestaurantAcceptEventPayload 생성에 실패했습니다. Order Id : "
                    + restaurantAcceptEventPayload.getOrderId());
        }
    }

    public Optional<RestaurantAcceptOutbox> getRestaurantAcceptOutboxBySagaIdAndSagaStatus(UUID sagaId,
                                                                                           SagaStatus sagaStatus) {
        return restaurantAcceptOutboxRepository.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, sagaId, sagaStatus);
    }

    @Transactional
    public int deleteOlderThan(ZonedDateTime threshold) {
        return restaurantAcceptOutboxRepository.deleteOlderThan(threshold);
    }
}
