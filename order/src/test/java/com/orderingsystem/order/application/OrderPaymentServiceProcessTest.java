package com.orderingsystem.order.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.ProcessedMessage;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.order.domain.repository.outbox.RestaurantApprovalOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPaymentServiceProcessTest {

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2025-08-02T19:15:00Z");
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID paymentOutboxId = UUID.randomUUID();
    private final OrderItem orderItem = OrderItem.builder()
            .productId(productId)
            .build();

    @BeforeEach
    void before() {
        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(paymentOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
    }

    @DisplayName("주문 결제에 성공한다.")
    @Test
    void orderPayment() {
        //given
        saveOrder(OrderStatus.PENDING);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);
        Optional<PaymentOutbox> paymentOutbox = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentOutbox).isPresent();
        assertThat(paymentOutbox.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
        assertThat(paymentOutbox.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @DisplayName("해당 주문 정보를 찾지 못하면 예외가 발생한다.")
    @Test
    void notFoundOrder() {
        //given
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when, then
        assertThatThrownBy(() -> orderPaymentService.process(paymentResponse))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문을 찾을 수 없습니다. Order Id : " + orderId);
    }

    @DisplayName("SagaStatus가 STARTED 상태인 PaymentOutbox가 없으면 처리하지 않는다.")
    @Test
    void shouldNotProcess_whenNoStartedPaymentOutboxExists() {
        //given
        saveOrder(OrderStatus.PENDING);
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(restaurantApprovalOutboxRepository.count()).isZero();
    }

    @DisplayName("ProcessedMessage에 이미 메시지가 저장되어 있으면 이미 해당 메시지가 처리 되었기에, 첫 호출도 즉시 스킵된다.")
    @Test
    void shouldSkipImmediately_whenAlreadyMarkedInProcessedMessage() {
        //given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PENDING);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(paymentResponse.getId())
                .messageType(MessageType.PAYMENT_COMPLETE)
                .processedAt(ZonedDateTime.now())
                .build());

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> savedOrder = orderRepository.findById(orderId);
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(restaurantApprovalOutboxRepository.count()).isZero();
    }

    @DisplayName("동일 메시지가 재전달되면, 이미 ProcessedMessage에 저장되어 있으므로 재처리하지 않는다.")
    @Test
    void shouldSkipProcessing_whenMessageAlreadyStoredInProcessedMessage() {
        // given
        saveOrder(OrderStatus.PENDING);
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        orderPaymentService.process(paymentResponse);

        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(restaurantApprovalOutboxRepository.count()).isEqualTo(1L);

        // when
        orderPaymentService.process(paymentResponse);

        // then
        Order savedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(restaurantApprovalOutboxRepository.count()).isEqualTo(1L);
    }

    @DisplayName("Saga Status가 STARTED 상태가 아니라면, 늦게 도착한 메시지는 무시된다.")
    @Test
    void shouldSkip_whenSagaStatusIsNotStarted() {
        // given
        saveOrder(OrderStatus.APPROVED);
        UUID sagaId = UUID.randomUUID();
        PaymentResponse resp = getPaymentResponse(sagaId, orderId);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.COMPENSATED)
                .orderStatus(OrderStatus.APPROVED)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        // when
        orderPaymentService.process(resp);

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(restaurantApprovalOutboxRepository.count()).isZero();
    }

    @DisplayName("해당 주문이 이미 승인된 상태라면, 주문 결제를 다시 처리하지 않는다.")
    @Test
    void AlreadyApprovedOrder() {
        //given
        saveOrder(OrderStatus.APPROVED);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @DisplayName("해당 주문이 이미 취소된 상태라면, 주문 결제를 다시 처리하지 않는다.")
    @Test
    void AlreadyCancelledOrder() {
        //given
        saveOrder(OrderStatus.CANCELLED);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("해당 주문이 이미 취소중인 상태라면, 주문 결제를 다시 처리하지 않는다.")
    @Test
    void AlreadyCancellingOrder() {
        //given
        saveOrder(OrderStatus.CANCELLING);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
    }

    private PaymentResponse getPaymentResponse(UUID sagaId, UUID orderId) {
        return PaymentResponse.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("100.00"))
                .createdAt(createdAt)
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .failureMessages(new ArrayList<>())
                .build();
    }

    private void saveOrder(OrderStatus orderStatus) {
        orderRepository.save(Order.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .trackingId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(orderItem))
                .orderStatus(orderStatus)
                .failureMessages("")
                .build());
    }
}
