package com.orderingsystem.restaurant.application.outbox.restaruantupdate;

import static com.orderingsystem.common.saga.SagaConstants.RESTAURANT_CREATE_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.model.RestaurantUpdateEventPayload;
import com.orderingsystem.restaurant.domain.exception.RestaurantDomainException;
import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
import com.orderingsystem.restaurant.domain.repository.outbox.RestaurantUpdateOutboxRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantUpdateOutboxHelper {

    private final RestaurantUpdateOutboxRepository restaurantUpdateOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(RestaurantUpdateOutbox restaurantUpdateOutbox) {
        if (!restaurantUpdateOutboxRepository.existsByTypeAndEventId(RESTAURANT_CREATE_NAME,
                restaurantUpdateOutbox.getEventId())) {
            restaurantUpdateOutboxRepository.save(restaurantUpdateOutbox);
            log.info("Restaurant Update Outbox 저장했습니다. Outbox Id : {}", restaurantUpdateOutbox.getId());
        } else {
            log.warn("이미 저장된 Restaurant Update Outbox가 존재합니다. Event Id : {}, Type : {}",
                    restaurantUpdateOutbox.getEventId(), restaurantUpdateOutbox.getType());
        }
    }

    @Transactional
    public void saveRestaurantUpdateOutboxMessage(RestaurantUpdateEventPayload restaurantUpdateEventPayload,
                                                  OutboxStatus outboxStatus, UUID eventId) {
        save(RestaurantUpdateOutbox.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .createdAt(restaurantUpdateEventPayload.getCreatedAt())
                .processedAt(ZonedDateTime.now())
                .type(RESTAURANT_CREATE_NAME)
                .payload(createPayload(restaurantUpdateEventPayload))
                .build());
    }

    private String createPayload(RestaurantUpdateEventPayload restaurantUpdateEventPayload) {
        try {
            return objectMapper.writeValueAsString(restaurantUpdateEventPayload);
        } catch (JsonProcessingException e) {
            log.error("RestaurantUpdateEventPayload 생성에 실패했습니다. Restaurant Id : {}",
                    restaurantUpdateEventPayload.getRestaurantId());
            throw new RestaurantDomainException(
                    "RestaurantUpdateEventPayload 생성에 실패했습니다. Restaurant Id : "
                            + restaurantUpdateEventPayload.getRestaurantId());
        }
    }

    @Transactional(readOnly = true)
    public List<RestaurantUpdateOutbox> getOrderOutboxMessage() {
        return restaurantUpdateOutboxRepository.findByType(RESTAURANT_CREATE_NAME);
    }

    @Transactional
    public void deleteAllOrderOutboxByOutboxStatus() {
        restaurantUpdateOutboxRepository.deleteAllByType(RESTAURANT_CREATE_NAME);
    }

}
