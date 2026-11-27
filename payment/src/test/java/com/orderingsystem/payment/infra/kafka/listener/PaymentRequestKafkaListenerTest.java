package com.orderingsystem.payment.infra.kafka.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.payment.application.PaymentService;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.infra.kafka.message.PaymentRequestDebeziumMessage;
import com.orderingsystem.payment.infra.kafka.message.PaymentRequestMessage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRequestKafkaListenerTest {

    private static final String MESSAGE = "message";
    private static final String PAYLOAD = "payload";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentRequestKafkaListener paymentRequestKafkaListener;

    private PaymentRequestDebeziumMessage debeziumMessage;
    private PaymentRequestDebeziumMessage.Payload after;

    @BeforeEach
    void setUp() throws Exception {
        debeziumMessage = mock(PaymentRequestDebeziumMessage.class);
        after = mock(PaymentRequestDebeziumMessage.Payload.class);

        given(objectMapper.readValue(MESSAGE, PaymentRequestDebeziumMessage.class)).willReturn(debeziumMessage);
        given(debeziumMessage.getOp()).willReturn(DebeziumOp.CREATE.getValue());
        given(debeziumMessage.getAfter()).willReturn(after);
        given(after.getPayload()).willReturn(PAYLOAD);
        given(objectMapper.readValue(PAYLOAD, PaymentRequestMessage.class)).willReturn(rollbackRequestMessage());
    }

    @Test
    @DisplayName("결제 rollback 메시지의 order_status가 REJECTING이면 REJECTING으로 cancelPayment를 호출한다")
    void shouldCallCancelPaymentWithRejecting_whenOrderStatusIsRejecting() {
        //given
        given(after.getOrderStatus()).willReturn(OrderStatus.REJECTING.name());

        //when
        paymentRequestKafkaListener.receive(MESSAGE, "key", 0, 0L);

        //then
        then(paymentService).should().cancelPayment(any(PaymentRequest.class), eq(OrderStatus.REJECTING));
    }

    @Test
    @DisplayName("결제 rollback 메시지의 order_status가 CANCELLING이면 CANCELLING으로 cancelPayment를 호출한다")
    void shouldCallCancelPaymentWithCancelling_whenOrderStatusIsCancelling() {
        //given
        given(after.getOrderStatus()).willReturn(OrderStatus.CANCELLING.name());

        //when
        paymentRequestKafkaListener.receive(MESSAGE, "key", 0, 0L);

        //then
        then(paymentService).should().cancelPayment(any(PaymentRequest.class), eq(OrderStatus.CANCELLING));
    }

    @Test
    @DisplayName("결제 rollback 메시지에 order_status가 없으면 PaymentApplicationException을 던지고 cancelPayment를 호출하지 않는다")
    void shouldThrowPaymentApplicationException_whenOrderStatusIsMissing() {
        //given
        given(after.getOrderStatus()).willReturn(null);

        //when, then
        assertThatThrownBy(() -> paymentRequestKafkaListener.receive(MESSAGE, "key", 0, 0L))
                .isInstanceOf(PaymentApplicationException.class)
                .hasMessageStartingWith("결제 rollback 메시지에 order_status가 없습니다.");

        then(paymentService).shouldHaveNoInteractions();
    }

    private PaymentRequestMessage rollbackRequestMessage() {
        return PaymentRequestMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .price(new BigDecimal("25.00"))
                .createdAt(OffsetDateTime.now())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED)
                .failureMessage(List.of())
                .build();
    }

}
