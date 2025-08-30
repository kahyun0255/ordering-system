package com.orderingsystem.payment.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.model.outbox.MessageType;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.model.outbox.ProcessedMessage;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.payment.domain.repository.outbox.ProcessedMessageRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PaymentServiceCancelTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CreditEntryRepository creditEntryRepository;

    @Autowired
    private CreditHistoryRepository creditHistoryRepository;

    @Autowired
    private OrderOutboxRepository orderOutboxRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAllInBatch();
        creditEntryRepository.deleteAllInBatch();
        creditEntryRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        creditEntryRepository.save(CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .totalCreditAmount(new Money(new BigDecimal("1000.00")))
                .build());

        creditHistoryRepository.save(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .type(TransactionType.CREDIT)
                .amount(new Money(new BigDecimal("1000.00")))
                .build());

        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .price(new Money(new BigDecimal("25.00")))
                .status(PaymentStatus.COMPLETED)
                .build());
    }

    @DisplayName("결제 취소에 성공한다.")
    @Test
    void cancelPayment() {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED)
                .build();
        //when
        Optional<Payment> beforePayment = paymentRepository.findByOrderId(orderId);
        assertThat(beforePayment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        paymentService.cancelPayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @DisplayName("결제가 된 금액이 0보다 작으면 결제 취소에 실패한다.")
    @Test
    void failToCancelPayment_whenTotalAmountIsNegative() throws JsonProcessingException {
        //given
        UUID orderId = UUID.randomUUID();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("-10.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED)
                .build();

        paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(orderId)
                .price(new Money(new BigDecimal("-25.00")))
                .status(PaymentStatus.COMPLETED)
                .build());

        //when
        paymentService.cancelPayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);

        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatus(
                ORDER_SAGA_NAME, sagaId, payment.get().getStatus());

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(List.of("총 가격은 0보다 커야합니다."));
    }

    @DisplayName("해당 결제 취소 요청을 이미 처리했고 성공적으로 처리 완료되었을 경우, 결제 로직을 진행하지 않는다.")
    @Test
    void failToCancelPayment_whenPaymentRequestAlreadyProcessed() {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        processedMessageRepository.save(ProcessedMessage.builder()
                        .messageId(paymentRequest.getId())
                        .messageType(MessageType.CANCEL_PAYMENT)
                        .processedAt(ZonedDateTime.now())
                .build());

        orderOutboxRepository.save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentStatus(PaymentStatus.CANCELLED)
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .build());

        //when
        paymentService.cancelPayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @DisplayName("해당 결제 취소 요청을 이미 처리했지만 성공적으로 완료되지 않았을 경우, 결제 로직을 진행한다.")
    @Test
    void shouldProceedWithPaymentLogic_whenCancelRequestProcessedButNotCompleted() {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(paymentRequest.getId())
                .messageType(MessageType.CANCEL_PAYMENT)
                .processedAt(ZonedDateTime.now())
                .build());

        orderOutboxRepository.save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentStatus(PaymentStatus.FAILED)
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .build());

        //when
        paymentService.cancelPayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @DisplayName("해당 결제 내역을 찾지 못하면 예외가 발생한다.")
    @Test
    void notFoundPayment() {
        //given
        UUID orderId = UUID.randomUUID();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        //when, then
        assertThatThrownBy(() -> paymentService.cancelPayment(paymentRequest))
                .isInstanceOf(PaymentApplicationException.class)
                .hasMessage("해당 주문에 대한 결제 정보를 찾지 못했습니다. Order Id : " + orderId);
    }

}
