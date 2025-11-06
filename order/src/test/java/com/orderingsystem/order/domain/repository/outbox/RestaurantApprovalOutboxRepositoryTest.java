package com.orderingsystem.order.domain.repository.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
class restaurantAcceptOutboxRepositoryTest {

    @Autowired
    private RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;

    private final String type = "ORDER_SAGA";

    @DisplayName("Type, SagaId, SagaStatus 조건에 맞는 RestaurantAcceptOutbox 객체들을 조회한다.")
    @Test
    void findByTypeAndSagaIdAndSagaStatusIn() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantAcceptOutbox RestaurantAcceptOutbox1 = getRestaurantAcceptOutbox(sagaId, SagaStatus.STARTED);
        RestaurantAcceptOutbox RestaurantAcceptOutbox2 = getRestaurantAcceptOutbox(sagaId, SagaStatus.SUCCEEDED);

        restaurantAcceptOutboxRepository.saveAll(List.of(RestaurantAcceptOutbox1, RestaurantAcceptOutbox2));

        //when
        Optional<RestaurantAcceptOutbox> result = restaurantAcceptOutboxRepository.findByTypeAndSagaIdAndSagaStatus(
                type, sagaId, SagaStatus.STARTED);

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getSagaStatus()).isEqualTo(SagaStatus.STARTED);
    }

    @DisplayName("조건에 맞는 RestaurantAcceptOutbox가 존재하면 True를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_True() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantAcceptOutbox RestaurantAcceptOutbox = getRestaurantAcceptOutbox(sagaId, OrderStatus.PENDING,
                SagaStatus.STARTED);

        restaurantAcceptOutboxRepository.saveAll(List.of(RestaurantAcceptOutbox));

        //when
        boolean result = restaurantAcceptOutboxRepository.existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(
                type, sagaId, OrderStatus.PENDING, SagaStatus.STARTED);

        //then
        assertThat(result).isTrue();
    }

    @DisplayName("조건에 맞는 RestaurantAcceptOutbox가 없으면 False를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_False() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantAcceptOutbox restaurantAcceptOutbox = getRestaurantAcceptOutbox(sagaId, OrderStatus.PENDING,
                SagaStatus.COMPENSATING);

        restaurantAcceptOutboxRepository.saveAll(List.of(restaurantAcceptOutbox));

        //when
        boolean result = restaurantAcceptOutboxRepository.existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(
                type, sagaId, OrderStatus.PENDING, SagaStatus.STARTED);

        //then
        assertThat(result).isFalse();
    }

    private RestaurantAcceptOutbox getRestaurantAcceptOutbox(UUID sagaId, SagaStatus sagaStatus) {
        return getRestaurantAcceptOutbox(sagaId, OrderStatus.APPROVED, sagaStatus);
    }

    private RestaurantAcceptOutbox getRestaurantAcceptOutbox(UUID sagaId, OrderStatus orderStatus, SagaStatus sagaStatus) {
        return RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(ZonedDateTime.now())
                .type(type)
                .payload("payload")
                .sagaStatus(sagaStatus)
                .orderStatus(orderStatus)
                .build();
    }
}
