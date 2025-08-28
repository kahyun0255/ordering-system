package com.orderingsystem.payment.application.outbox.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
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
class OrderOutboxCleanerSchedulerTest {

    @Autowired
    private OrderOutboxCleanerScheduler orderOutboxCleanerScheduler;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @DisplayName("OutboxStatus가 COMPLETED인 Order Outbox 메시지들을 삭제한다.")
    @Test
    void deleteCompletedPaymentOutboxMessages() {
        //given
        OrderOutbox orderOutbox1 = getOrderOutbox(OutboxStatus.COMPLETED);
        OrderOutbox orderOutbox2 = getOrderOutbox(OutboxStatus.COMPLETED);
        OrderOutbox orderOutbox3 = getOrderOutbox(OutboxStatus.COMPLETED);

        OrderOutbox orderOutbox4 = getOrderOutbox(OutboxStatus.STARTED);
        OrderOutbox orderOutbox5 = getOrderOutbox(OutboxStatus.STARTED);

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

    @DisplayName("삭제할 COMPLETED 상태의 메시지가 없으면 아무 동작도 하지 않는다.")
    @Test
    void doNothing_whenNoCompletedMessagesExist() {
        //given
        assertThat(orderOutboxRepository.count()).isZero();

        //when
        orderOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = orderOutboxRepository.count();
        assertThat(afterCount).isZero();
    }

    private static OrderOutbox getOrderOutbox(OutboxStatus outboxStatus) {
        return OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .createdAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();
    }
}
