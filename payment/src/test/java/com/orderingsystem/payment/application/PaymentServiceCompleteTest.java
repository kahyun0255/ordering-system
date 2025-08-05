package com.orderingsystem.payment.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.application.dto.request.PaymentRequest;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.domain.model.CreditEntry;
import com.orderingsystem.payment.domain.model.CreditHistory;
import com.orderingsystem.payment.domain.model.Payment;
import com.orderingsystem.payment.domain.model.TransactionType;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.domain.repository.CreditEntryRepository;
import com.orderingsystem.payment.domain.repository.CreditHistoryRepository;
import com.orderingsystem.payment.domain.repository.PaymentRepository;
import com.orderingsystem.payment.domain.repository.outbox.OrderOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class PaymentServiceCompleteTest {

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
    }

    @DisplayName("결제에 성공한다.")
    @Test
    void paymentComplete() throws JsonProcessingException {
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

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, sagaId, PaymentStatus.COMPLETED, OutboxStatus.STARTED);

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(List.of());
    }

    @DisplayName("잔액이 부족할 경우 결제에 실패한다.")
    @Test
    void failToCompletePayment_whenBalanceIsInsufficient() throws JsonProcessingException {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("100000.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, sagaId, PaymentStatus.FAILED, OutboxStatus.STARTED);

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("고객의 크레딧이 결제 금액보다 부족합니다.",
                        "Credit History 기준으로 크레딧이 부족합니다."));
    }

    @DisplayName("Credit Entry와 Credit History가 일치하지 않을 경우 결제에 실패한다.")
    @Test
    void failToCompletePayment_whenCreditEntryAndHistoryMismatch() throws JsonProcessingException {
        //given
        creditHistoryRepository.save(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .type(TransactionType.DEBIT)
                .amount(new Money(new BigDecimal("100.00")))
                .build());

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("20.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, sagaId, PaymentStatus.FAILED, OutboxStatus.STARTED);

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("Credit History 이력 총합이 현재 크레딧과 일치하지 않습니다."));
    }

    @DisplayName("결제를 요청한 금액이 0이면 결제에 실패한다.")
    @Test
    void failToCompletePayment_whenRequestedAmountIsZero() throws JsonProcessingException {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, sagaId, PaymentStatus.FAILED, OutboxStatus.STARTED);

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("총 가격은 0보다 커야합니다."));
    }

    @DisplayName("결제를 요청한 금액이 0보다 작으면 결제에 실패한다.")
    @Test
    void failToCompletePayment_whenRequestedAmountIsNegative() throws JsonProcessingException {
        //given
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("-100.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
        Optional<OrderOutbox> orderOutbox = orderOutboxRepository.findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(
                ORDER_SAGA_NAME, sagaId, PaymentStatus.FAILED, OutboxStatus.STARTED);

        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().getPayload();
        OrderEventPayload orderEventPayload = objectMapper.readValue(payload, OrderEventPayload.class);

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("총 가격은 0보다 커야합니다."));
    }

    @DisplayName("결제를 요청한 Customer에 대한 CreditEntry 정보가 없으면 예외가 발생한다.")
    @Test
    void failToCompletePayment_whenCreditEntryNotFoundForCustomer() {
        //given
        UUID customerId = UUID.randomUUID();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        creditHistoryRepository.save(CreditHistory.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .type(TransactionType.CREDIT)
                .amount(new Money(new BigDecimal("1000.00")))
                .build());

        //when, then
        assertThatThrownBy(() -> paymentService.completePayment(paymentRequest))
                .isInstanceOf(PaymentApplicationException.class)
                .hasMessage("해당 고객에 대한 CreditEntry 정보를 찾을 수 없습니다. Customer Id : " + customerId);
    }

    @DisplayName("결제를 요청한 Customer에 대한 CreditHistory 정보가 없으면 예외가 발생한다.")
    @Test
    void failToCompletePayment_whenCreditHistoryNotFoundForCustomer() {
        //given
        UUID customerId = UUID.randomUUID();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .paymentOrderStatus(PaymentOrderStatus.PENDING)
                .build();

        creditEntryRepository.save(CreditEntry.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .totalCreditAmount(new Money(new BigDecimal("1000.00")))
                .build());

        //when, then
        assertThatThrownBy(() -> paymentService.completePayment(paymentRequest))
                .isInstanceOf(PaymentApplicationException.class)
                .hasMessage("해당 고객에 대한 CreditHistory 정보를 찾을 수 없습니다. Customer Id : " + customerId);
    }

    @DisplayName("해당 결제 요청을 이미 처리했을 경우, 결제 로직을 진행하지 않는다.")
    @Test
    void failToCompletePayment_whenPaymentRequestAlreadyProcessed() {
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

        orderOutboxRepository.save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentStatus(PaymentStatus.COMPLETED)
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .outboxStatus(OutboxStatus.COMPLETED)
                .build());

        //when
        paymentService.completePayment(paymentRequest);

        //then
        Optional<Payment> payment = paymentRepository.findByOrderId(orderId);

        assertThat(paymentRepository.count()).isEqualTo(0L);
        assertThat(payment).isEmpty();
    }

}
