package com.orderingsystem.order.domain.repository.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.outbox.OutboxStatus;
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
class RestaurantApprovalOutboxRepositoryTest {

    @Autowired
    private RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;

    private final String type = "ORDER_SAGA";

    @DisplayName("Type, SagaId, SagaStatus 조건에 맞는 RestaurantApprovalOutbox 객체들을 조회한다.")
    @Test
    void findByTypeAndSagaIdAndSagaStatusIn() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantApprovalOutbox restaurantApprovalOutbox1 = getRestaurantApprovalOutbox(sagaId, SagaStatus.STARTED);
        RestaurantApprovalOutbox restaurantApprovalOutbox2 = getRestaurantApprovalOutbox(sagaId, SagaStatus.SUCCEEDED);

        restaurantApprovalOutboxRepository.saveAll(List.of(restaurantApprovalOutbox1, restaurantApprovalOutbox2));

        //when
        Optional<RestaurantApprovalOutbox> result = restaurantApprovalOutboxRepository.findByTypeAndSagaIdAndSagaStatus(
                type, sagaId, SagaStatus.STARTED);

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getSagaStatus()).isEqualTo(SagaStatus.STARTED);
    }

    @DisplayName("조건에 맞는 RestaurantApprovalOutbox가 존재하면 True를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_True() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantApprovalOutbox restaurantApprovalOutbox = getRestaurantApprovalOutbox(sagaId, OrderStatus.PENDING,
                SagaStatus.STARTED);

        restaurantApprovalOutboxRepository.saveAll(List.of(restaurantApprovalOutbox));

        //when
        boolean result = restaurantApprovalOutboxRepository.existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(
                type, sagaId, OrderStatus.PENDING, SagaStatus.STARTED);

        //then
        assertThat(result).isTrue();
    }

    @DisplayName("조건에 맞는 RestaurantApprovalOutbox가 없으면 False를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_False() {
        //given
        UUID sagaId = UUID.randomUUID();
        RestaurantApprovalOutbox restaurantApprovalOutbox = getRestaurantApprovalOutbox(sagaId, OrderStatus.PENDING,
                SagaStatus.COMPENSATING);

        restaurantApprovalOutboxRepository.saveAll(List.of(restaurantApprovalOutbox));

        //when
        boolean result = restaurantApprovalOutboxRepository.existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(
                type, sagaId, OrderStatus.PENDING, SagaStatus.STARTED);

        //then
        assertThat(result).isFalse();
    }

    private RestaurantApprovalOutbox getRestaurantApprovalOutbox(UUID sagaId, SagaStatus sagaStatus) {
        return getRestaurantApprovalOutbox(sagaId, OutboxStatus.COMPLETED, OrderStatus.APPROVED, sagaStatus);
    }

    private RestaurantApprovalOutbox getRestaurantApprovalOutbox(OutboxStatus outboxStatus, SagaStatus sagaStatus) {
        return getRestaurantApprovalOutbox(UUID.randomUUID(), outboxStatus, OrderStatus.APPROVED, sagaStatus);
    }

    private RestaurantApprovalOutbox getRestaurantApprovalOutbox(UUID sagaId, OrderStatus orderStatus,
                                                                 SagaStatus sagaStatus) {
        return getRestaurantApprovalOutbox(sagaId, OutboxStatus.COMPLETED, orderStatus, sagaStatus);
    }

    private RestaurantApprovalOutbox getRestaurantApprovalOutbox(UUID sagaId, OutboxStatus outboxStatus,
                                                                 OrderStatus orderStatus, SagaStatus sagaStatus) {
        return RestaurantApprovalOutbox.builder()
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
