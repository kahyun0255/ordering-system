package com.orderingsystem.order.application.outbox.restaurant.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.outbox.RestaurantApprovalOutboxRepository;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RestaurantApprovalOutboxCleanerSchedulerTest {

    @Autowired
    private RestaurantApprovalOutboxCleanerScheduler restaurantApprovalOutboxCleanerScheduler;

    @Autowired
    private RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;

    @DisplayName("OutboxStatus가 COMPLETED이고, SagaStatus가 SUCCEEDED이거나 FAILED, COMPENSATED인 메시지만 삭제한다.")
    @Test
    void deleteCompletedRestaurantApprovalOutboxMessages() {
        //given
        RestaurantApprovalOutbox restaurantApprovalOutbox1 = getRestaurantApprovalOutbox(SagaStatus.SUCCEEDED);
        RestaurantApprovalOutbox restaurantApprovalOutbox2 = getRestaurantApprovalOutbox(SagaStatus.FAILED);
        RestaurantApprovalOutbox restaurantApprovalOutbox3 = getRestaurantApprovalOutbox(SagaStatus.COMPENSATED);

        RestaurantApprovalOutbox restaurantApprovalOutbox4 = getRestaurantApprovalOutbox(SagaStatus.STARTED);
        RestaurantApprovalOutbox restaurantApprovalOutbox5 = getRestaurantApprovalOutbox(SagaStatus.COMPENSATING);

        restaurantApprovalOutboxRepository.saveAll(
                List.of(restaurantApprovalOutbox1, restaurantApprovalOutbox2, restaurantApprovalOutbox3,
                        restaurantApprovalOutbox4, restaurantApprovalOutbox5));

        //when
        long beforeCount = restaurantApprovalOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);
        restaurantApprovalOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = restaurantApprovalOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private static RestaurantApprovalOutbox getRestaurantApprovalOutbox(SagaStatus sagaStatus) {
        return RestaurantApprovalOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .createdAt(ZonedDateTime.now())
                .processedAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .sagaStatus(sagaStatus)
                .orderStatus(OrderStatus.APPROVED)
                .outboxStatus(OutboxStatus.COMPLETED)
                .build();
    }

}
