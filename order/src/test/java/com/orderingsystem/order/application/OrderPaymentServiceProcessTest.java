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
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.ProcessedMessage;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.CouponOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.order.domain.repository.outbox.RestaurantAcceptOutboxRepository;
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
    private CouponOutboxRepository couponOutboxRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;

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

    @DisplayName("쿠폰이 없는 주문의 경우, 결제 완료 시 즉시 레스토랑 요청을 보낸다.")
    @Test
    void process_success_no_coupon() {
        //given
        saveOrder(OrderStatus.PENDING, null);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);

        Optional<PaymentOutbox> paymentOutbox = paymentOutboxRepository.findById(paymentOutboxId);
        assertThat(paymentOutbox).isPresent();
        assertThat(paymentOutbox.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
        assertThat(restaurantAcceptOutboxRepository.count()).isEqualTo(1);
    }

    @DisplayName("쿠폰이 있는 주문이지만 쿠폰 처리가 미완료인 경우, 결제만 완료하고 레스토랑 요청은 대기한다.")
    @Test
    void process_wait_when_coupon_not_processed() {
        //given
        saveOrder(OrderStatus.PENDING, List.of(1L));
        saveCouponOutbox(SagaStatus.STARTED);

        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);

        assertThat(restaurantAcceptOutboxRepository.count()).isZero();
    }

    @DisplayName("쿠폰이 있는 주문이고, 쿠폰 처리가 완료되었을 경우, 결제 완료 후 레스토랑 요청을 보낸다.")
    @Test
    void process_proceed_when_coupon_processed() {
        //given
        saveOrder(OrderStatus.PENDING, List.of(1L));
        saveCouponOutbox(SagaStatus.SUCCEEDED);

        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);

        assertThat(restaurantAcceptOutboxRepository.count()).isEqualTo(1);
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
        saveOrder(OrderStatus.PENDING, null);
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
        assertThat(restaurantAcceptOutboxRepository.count()).isZero();
    }

    @DisplayName("ProcessedMessage에 이미 메시지가 저장되어 있으면 이미 해당 메시지가 처리 되었기에, 즉시 스킵된다.")
    @Test
    void shouldSkipImmediately_whenAlreadyMarkedInProcessedMessage() {
        //given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PENDING, null);
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
        assertThat(restaurantAcceptOutboxRepository.count()).isZero();
    }

    @DisplayName("Saga Status가 STARTED 상태가 아니라면, 늦게 도착한 메시지는 무시된다.")
    @Test
    void shouldSkip_whenSagaStatusIsNotStarted() {
        //given
        saveOrder(OrderStatus.APPROVED, null);
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

        //when
        orderPaymentService.process(resp);

        //then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(restaurantAcceptOutboxRepository.count()).isZero();
    }

    @DisplayName("해당 주문이 이미 승인된 상태라면, 주문 결제를 다시 처리하지 않는다.")
    @Test
    void AlreadyApprovedOrder() {
        //given
        saveOrder(OrderStatus.APPROVED, null);
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
        saveOrder(OrderStatus.CANCELLED, null);
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        //when
        orderPaymentService.process(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
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

    private void saveOrder(OrderStatus orderStatus, List<Long> couponIds) {
        orderRepository.save(Order.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .trackingId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(orderItem))
                .orderStatus(orderStatus)
                .couponIds(couponIds)
                .failureMessages("")
                .build());
    }

    private void saveCouponOutbox(SagaStatus status) {
        couponOutboxRepository.save(CouponOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(status)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .processedAt(ZonedDateTime.now())
                .build());
    }

}
