package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.outbox.RestaurantApprovalOutboxRepository;
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
class RestaurantApprovalOutboxCleanerSchedulerTest {

    @Autowired
    private RestaurantApprovalOutboxCleanerScheduler restaurantApprovalOutboxCleanerScheduler;

    @Autowired
    private RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;

    @DisplayName("Delete TTL 이전에 생성한 Outbox Message를 삭제한다.")
    @Test
    void shouldDeleteOutboxMessagesCreatedBeforeDeleteTTL() {
        //given
        RestaurantApprovalOutbox outbox1 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(4));
        RestaurantApprovalOutbox outbox2 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(3));
        RestaurantApprovalOutbox outbox3 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(3));

        RestaurantApprovalOutbox outbox4 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(1));
        RestaurantApprovalOutbox outbox5 = getRestaurantApprovalOutbox(ZonedDateTime.now().minusDays(2));

        restaurantApprovalOutboxRepository.saveAll(List.of(outbox1, outbox2, outbox3, outbox4, outbox5));

        long beforeCount = restaurantApprovalOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);

        //when
        restaurantApprovalOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = restaurantApprovalOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private RestaurantApprovalOutbox getRestaurantApprovalOutbox(ZonedDateTime threshold) {
        return RestaurantApprovalOutbox.builder()
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
