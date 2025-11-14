package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import com.orderingsystem.order.domain.repository.outbox.RestaurantAcceptOutboxRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@TestPropertySource(properties = "outbox.delete-ttl=3")
class RestaurantAcceptOutboxCleanerSchedulerTest {

    @Autowired
    private RestaurantAcceptOutboxCleanerScheduler restaurantAcceptOutboxCleanerScheduler;

    @Autowired
    private RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;

    @DisplayName("Delete TTL 이전에 생성한 Outbox Message를 삭제한다.")
    @Test
    void shouldDeleteOutboxMessagesCreatedBeforeDeleteTTL() {
        //given
        RestaurantAcceptOutbox outbox1 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(4));
        RestaurantAcceptOutbox outbox2 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(3));
        RestaurantAcceptOutbox outbox3 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(3));

        RestaurantAcceptOutbox outbox4 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(1));
        RestaurantAcceptOutbox outbox5 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(2));

        restaurantAcceptOutboxRepository.saveAll(List.of(outbox1, outbox2, outbox3, outbox4, outbox5));

        long beforeCount = restaurantAcceptOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);

        //when
        restaurantAcceptOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = restaurantAcceptOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private RestaurantAcceptOutbox getRestaurantApprovalOutbox(ZonedDateTime threshold) {
        return RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .createdAt(threshold)
                .type(ORDER_SAGA_NAME)
                .sagaStatus(SagaStatus.PROCESSING)
                .orderStatus(OrderStatus.PAID)
                .payload("payload")
                .build();
    }

}
