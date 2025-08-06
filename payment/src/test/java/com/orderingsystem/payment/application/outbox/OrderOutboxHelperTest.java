package com.orderingsystem.payment.application.outbox;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.exception.PaymentDomainException;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OrderOutboxHelperTest {

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @InjectMocks
    private OrderOutboxHelper orderOutboxHelper;

    @Mock
    private ObjectMapper objectMapper;

    @DisplayName("이미 저장된 Outbox 메시지가 존재하면 저장하지 않는다.")
    @Test
    void doNotSaveWhenOutboxMessageAlreadyExists() {
        // given
        UUID sagaId = UUID.randomUUID();
        OrderOutbox existingOutbox = OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentStatus(PaymentStatus.COMPLETED)
                .outboxStatus(OutboxStatus.STARTED)
                .createdAt(ZonedDateTime.now())
                .processedAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .build();

        given(orderOutboxRepository.existsByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                anyString(), any(), any(), any())).willReturn(true);

        // when
        orderOutboxHelper.save(existingOutbox);

        // then
        verify(orderOutboxRepository, never()).save(any());
    }

    @DisplayName("OrderEventPayload 직렬화 실패 시 예외를 던진다.")
    @Test
    void throwException_whenPayloadSerializationFails() throws JsonProcessingException {
        //given
        OrderEventPayload payload = OrderEventPayload.builder()
                .paymentId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId(UUID.randomUUID().toString())
                .sagaId(UUID.randomUUID().toString())
                .build();

        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        //when, then
        assertThatThrownBy(() -> orderOutboxHelper.saveOrderOutboxMessage(
                payload, PaymentStatus.COMPLETED, OutboxStatus.STARTED, UUID.randomUUID()))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("OrderEventPayload 생성에 실패했습니다");
    }

}
