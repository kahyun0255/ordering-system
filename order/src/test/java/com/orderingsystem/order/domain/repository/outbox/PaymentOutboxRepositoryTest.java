package com.orderingsystem.order.domain.repository.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PaymentOutboxRepositoryTest {

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    private final String type = "ORDER_SAGA";

    @DisplayName("Type, SagaId, SagaStatus 조건에 맞는 Payment Outbox 객체들을 조회한다.")
    @Test
    void findByTypeAndSagaIdAndSagaStatusIn() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentOutbox paymentOutbox1 = getPaymentOutbox(sagaId, SagaStatus.STARTED);
        PaymentOutbox paymentOutbox2 = getPaymentOutbox(sagaId, SagaStatus.SUCCEEDED);

        paymentOutboxRepository.saveAll(List.of(paymentOutbox1, paymentOutbox2));

        //when
        Optional<PaymentOutbox> result = paymentOutboxRepository.findByTypeAndSagaIdAndSagaStatusIn(
                type, sagaId, List.of(SagaStatus.STARTED, SagaStatus.COMPENSATING));

        //then
        assertThat(result).isPresent();
        assertThat(result.get().getSagaStatus()).isEqualTo(SagaStatus.STARTED);
    }

    @DisplayName("조건에 맞는 Payment Outbox가 존재하면 True를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_True() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentOutbox paymentOutbox = getPaymentOutbox(sagaId, SagaStatus.STARTED);

        paymentOutboxRepository.saveAll(List.of(paymentOutbox));

        //when
        boolean result = paymentOutboxRepository.existsByTypeAndSagaIdAndSagaStatus(
                type, sagaId, SagaStatus.STARTED);

        //then
        assertThat(result).isTrue();
    }

    @DisplayName("조건에 맞는 Payment Outbox가 없으면 False를 반환한다.")
    @Test
    void existsByTypeAndSagaIdAndSagaStatusAndOutboxStatus_False() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentOutbox paymentOutbox = getPaymentOutbox(sagaId, SagaStatus.COMPENSATING);

        paymentOutboxRepository.saveAll(List.of(paymentOutbox));

        //when
        boolean result = paymentOutboxRepository.existsByTypeAndSagaIdAndSagaStatus(
                type, sagaId, SagaStatus.STARTED);

        //then
        assertThat(result).isFalse();
    }

    private PaymentOutbox getPaymentOutbox(UUID sagaId, SagaStatus sagaStatus) {
        return PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(ZonedDateTime.now())
                .type(type)
                .payload("payload")
                .sagaStatus(sagaStatus)
                .orderStatus(OrderStatus.APPROVED)
                .build();
    }

}
