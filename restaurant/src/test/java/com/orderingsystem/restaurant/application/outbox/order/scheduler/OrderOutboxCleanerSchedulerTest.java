package com.orderingsystem.restaurant.application.outbox.order.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
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
class OrderOutboxCleanerSchedulerTest {

    @Autowired
    private OrderOutboxCleanerScheduler orderOutboxCleanerScheduler;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @DisplayName("Delete TTL 이전에 생성한 Outbox Message를 삭제한다.")
    @Test
    void shouldDeleteOutboxMessagesCreatedBeforeDeleteTTL() {
        //given
        OrderOutbox orderOutbox1 = getOrderOutbox(ZonedDateTime.now().minusDays(4));
        OrderOutbox orderOutbox2 = getOrderOutbox(ZonedDateTime.now().minusDays(3));
        OrderOutbox orderOutbox3 = getOrderOutbox(ZonedDateTime.now().minusDays(3));

        OrderOutbox orderOutbox4 = getOrderOutbox(ZonedDateTime.now().minusDays(1));
        OrderOutbox orderOutbox5 = getOrderOutbox(ZonedDateTime.now().minusDays(2));

        orderOutboxRepository.saveAll(
                List.of(orderOutbox1, orderOutbox2, orderOutbox3, orderOutbox4, orderOutbox5));

        long beforeCount = orderOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);

        //when
        orderOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = orderOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private static OrderOutbox getOrderOutbox(ZonedDateTime createdAt) {
        return OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .createdAt(createdAt)
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .build();
    }
}