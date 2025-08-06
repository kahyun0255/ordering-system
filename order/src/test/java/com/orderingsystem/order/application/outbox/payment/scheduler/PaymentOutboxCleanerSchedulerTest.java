package com.orderingsystem.order.application.outbox.payment.scheduler;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
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
class PaymentOutboxCleanerSchedulerTest {

    @Autowired
    private PaymentOutboxCleanerScheduler paymentOutboxCleanerScheduler;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @DisplayName("OutboxStatus가 COMPLETED이고, SagaStatus가 SUCCEDDED이거나 FAILED, COMPENSATED인 PaymentOutbox 메시지만 삭제한다.")
    @Test
    void deleteCompletedPaymentOutboxMessages() {
        //given
        PaymentOutbox paymentOutbox1 = getPaymentOutbox(SagaStatus.SUCCEEDED);
        PaymentOutbox paymentOutbox2 = getPaymentOutbox(SagaStatus.FAILED);
        PaymentOutbox paymentOutbox3 = getPaymentOutbox(SagaStatus.COMPENSATED);

        PaymentOutbox paymentOutbox4 = getPaymentOutbox(SagaStatus.STARTED);
        PaymentOutbox paymentOutbox5 = getPaymentOutbox(SagaStatus.PROCESSING);

        paymentOutboxRepository.saveAll(
                List.of(paymentOutbox1, paymentOutbox2, paymentOutbox3, paymentOutbox4, paymentOutbox5));

        //when
        long beforeCount = paymentOutboxRepository.count();
        assertThat(beforeCount).isEqualTo(5L);
        paymentOutboxCleanerScheduler.processOutboxMessage();

        //then
        long afterCount = paymentOutboxRepository.count();
        assertThat(afterCount).isEqualTo(2L);
    }

    private static PaymentOutbox getPaymentOutbox(SagaStatus sagaStatus) {
        return PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .createdAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .orderStatus(OrderStatus.APPROVED)
                .sagaStatus(sagaStatus)
                .outboxStatus(OutboxStatus.COMPLETED)
                .build();
    }

}
