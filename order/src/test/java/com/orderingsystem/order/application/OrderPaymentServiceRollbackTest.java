package com.orderingsystem.order.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.ProductOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.CouponOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.order.domain.repository.outbox.ProductOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.RestaurantAcceptOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPaymentServiceRollbackTest {

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;

    @Autowired
    private CouponOutboxRepository couponOutboxRepository;

    @Autowired
    private ProductOutboxRepository productOutboxRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2025-08-02T19:15:00Z");
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final OrderItem orderItem = OrderItem.builder()
            .productId(productId)
            .build();

    @AfterEach
    void tearDown() {
        paymentOutboxRepository.deleteAllInBatch();
        processedMessageRepository.deleteAllInBatch();
        couponOutboxRepository.deleteAllInBatch();
        productOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("주문 상태가 PENDING이면 주문 취소에 성공한다.")
    @Test
    void cancelOrder_whenOrderStatusIsPending() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);
        saveOrder(OrderStatus.PENDING);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.SUCCEEDED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(null)
                .type("OrderProcessingSaga")
                .sagaStatus(SagaStatus.COMPENSATING)
                .payload("{}")
                .orderStatus(OrderStatus.CANCELLING)
                .build());

        //when
        orderPaymentService.rollback(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("주문 상태가 CANCELLING이면 주문 취소에 성공한다.")
    @Test
    void cancelOrder_whenOrderStatusIsCANCELLING() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);
        saveOrder(OrderStatus.CANCELLING);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.SUCCEEDED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(null)
                .type("OrderProcessingSaga")
                .sagaStatus(SagaStatus.COMPENSATING)
                .payload("{}")
                .orderStatus(OrderStatus.CANCELLING)
                .build());

        //when
        orderPaymentService.rollback(paymentResponse);

        //then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("주문이 이미 승인되었으면 주문 취소에 실패한다.")
    @Test
    void cancelOrder_whenOrderStatusIsAPPROVED() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);
        saveOrder(OrderStatus.APPROVED);

        //when
        orderPaymentService.rollback(paymentResponse);

        // then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @DisplayName("주문이 이미 취소되었으면 주문 취소에 실패한다.")
    @Test
    void cancelOrder_whenOrderStatusIsCANCELLED() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);
        saveOrder(OrderStatus.CANCELLED);

        //when
        orderPaymentService.rollback(paymentResponse);

        // then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("주문 상태가 PAID이면 주문 취소에 성공하고 쿠폰 보상 Outbox가 저장된다.")
    @Test
    void cancelOrder_whenOrderStatusIsPAID_withCoupon() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId);

        saveOrder(OrderStatus.PAID, List.of(1L, 2L));

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.SUCCEEDED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        //when
        orderPaymentService.rollback(paymentResponse);

        // then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        Optional<CouponOutbox> couponOutbox = couponOutboxRepository.findBySagaIdAndSagaStatus(sagaId, SagaStatus.COMPENSATED);
        assertThat(couponOutbox).isPresent();

        Optional<ProductOutbox> productOutbox = productOutboxRepository.findBySagaIdAndSagaStatus(sagaId, SagaStatus.COMPENSATED);
        assertThat(productOutbox).isPresent();
    }

    @DisplayName("결제 상태가 FAILED이면 SagaStatus는 STARTED 또는 PROCESSING인 PaymentOutbox를 찾아야 한다")
    @Test
    void rollback_whenPaymentStatusIsFAILED_shouldUseStartedOrProcessingSagaStatus() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId, PaymentStatus.FAILED);

        saveOrder(OrderStatus.PENDING);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(null)
                .type("OrderProcessingSaga")
                .sagaStatus(SagaStatus.COMPENSATING)
                .payload("{}")
                .orderStatus(OrderStatus.CANCELLING)
                .build());

        // when
        orderPaymentService.rollback(paymentResponse);

        // then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("결제 상태가 COMPLETED이면 SagaStatus는 STARTED인 PaymentOutbox를 찾아야 한다")
    @Test
    void rollback_whenPaymentStatusIsCOMPLETED_shouldUseStartedSagaStatus() {
        //given
        UUID sagaId = UUID.randomUUID();
        PaymentResponse paymentResponse = getPaymentResponse(sagaId, orderId, PaymentStatus.COMPLETED);

        saveOrder(OrderStatus.PENDING);

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(null)
                .type("OrderProcessingSaga")
                .sagaStatus(SagaStatus.COMPENSATING)
                .payload("{}")
                .orderStatus(OrderStatus.CANCELLING)
                .build());

        // when
        orderPaymentService.rollback(paymentResponse);

        // then
        Optional<Order> order = orderRepository.findById(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private void saveOrder(OrderStatus orderStatus) {
        saveOrder(orderStatus, null);
    }

    private void saveOrder(OrderStatus orderStatus, List<Long> couponIds) {
        orderRepository.save(Order.builder()
                .id(orderId)
                .customerId(customerId)
                .orderStatus(orderStatus)
                .restaurantId(restaurantId)
                .address(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .price(new Money(new BigDecimal("25.00")))
                .items(List.of(orderItem))
                .couponIds(couponIds)
                .build());
    }

    private PaymentResponse getPaymentResponse(UUID sagaId, UUID orderId) {
        return getPaymentResponse(sagaId, orderId, PaymentStatus.CANCELLED);
    }

    private PaymentResponse getPaymentResponse(UUID sagaId, UUID orderId, PaymentStatus paymentStatus) {
        return PaymentResponse.builder()
                .sagaId(sagaId)
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("100.00"))
                .createdAt(createdAt)
                .paymentStatus(paymentStatus.name())
                .failureMessages(new ArrayList<>())
                .build();
    }

}
