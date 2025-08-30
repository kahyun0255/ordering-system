package com.orderingsystem.order.application.outbox.payment.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
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
class PaymentOutboxCleanerSchedulerTest {

    @Autowired
    private PaymentOutboxCleanerScheduler paymentOutboxCleanerScheduler;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @DisplayName("Delete TTL 이전에 생성한 Outbox Message를 삭제한다.")
    @Test
    void shouldDeleteOutboxMessagesCreatedBeforeDeleteTTL() {
        //given
        PaymentOutbox orderOutbox1 = getPaymentOutbox(ZonedDateTime.now().minusDays(4));
        PaymentOutbox orderOutbox2 = getPaymentOutbox(ZonedDateTime.now().minusDays(3));
        PaymentOutbox orderOutbox3 = getPaymentOutbox(ZonedDateTime.now().minusDays(3));

        PaymentOutbox orderOutbox4 = getPaymentOutbox(ZonedDateTime.now().minusDays(1));
        PaymentOutbox orderOutbox5 = getPaymentOutbox(ZonedDateTime.now().minusDays(2));

        paymentOutboxRepository.saveAll(
                List.of(orderOutbox1, orderOutbox2, orderOutbox3, orderOutbox4, orderOutbox5));

        long beforeCount = paymentOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);

        //when
        paymentOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = paymentOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private PaymentOutbox getPaymentOutbox(ZonedDateTime threshold) {
        return PaymentOutbox.builder()
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
