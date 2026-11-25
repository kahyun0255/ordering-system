package com.orderingsystem.order.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.CouponResponse;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.CouponOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.order.domain.repository.outbox.ProductOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
class OrderCouponServiceRollbackTest {

    @Autowired
    private OrderCouponService orderCouponService;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponOutboxRepository couponOutboxRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private ProductOutboxRepository productOutboxRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final OrderItem orderItem = OrderItem.builder()
            .productId(UUID.randomUUID())
            .build();
    private final List<String> couponFailureMessages = List.of("쿠폰이 사용 불가능한 상태입니다.");

    @AfterEach
    void tearDown() {
        paymentOutboxRepository.deleteAllInBatch();
        couponOutboxRepository.deleteAllInBatch();
        productOutboxRepository.deleteAllInBatch();
        processedMessageRepository.deleteAllInBatch();
    }

    @DisplayName("결제 응답 전(PENDING)에 쿠폰 사용 실패 응답을 받으면, 주문을 CANCELLING으로 바꾸고 결제 취소 요청 PaymentOutbox를 저장하며 CouponOutbox를 FAILED로 바꾼다.")
    @Test
    void shouldCancelOrderAndRequestPaymentCancel_whenCouponFailedBeforePaymentResponse() {
        // given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PENDING);
        CouponOutbox couponOutbox = saveCouponOutbox(sagaId, SagaStatus.STARTED, OrderStatus.PENDING);

        // when
        orderCouponService.rollback(getCouponFailureResponse(sagaId));

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(order.getFailureMessageList()).containsAll(couponFailureMessages);

        assertThat(couponOutboxRepository.findById(couponOutbox.getId()).orElseThrow().getSagaStatus())
                .isEqualTo(SagaStatus.FAILED);

        PaymentOutbox cancelRequestOutbox = paymentOutboxRepository
                .findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId, List.of(SagaStatus.FAILED))
                .orElseThrow();
        assertThat(cancelRequestOutbox.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
    }

    @DisplayName("결제가 먼저 완료(PAID)된 뒤 쿠폰 사용 실패 응답을 받으면, 주문을 CANCELLING으로 바꾸고 결제 취소 요청 PaymentOutbox를 저장한다.")
    @Test
    void shouldCancelOrderAndRequestPaymentCancel_whenCouponFailedAfterPaymentCompleted() {
        // given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PAID);
        saveCouponOutbox(sagaId, SagaStatus.STARTED, OrderStatus.PENDING);
        savePaymentOutbox(sagaId, SagaStatus.PROCESSING, OrderStatus.PAID);

        // when
        orderCouponService.rollback(getCouponFailureResponse(sagaId));

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);

        assertThat(paymentOutboxRepository
                .findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId, List.of(SagaStatus.FAILED)))
                .isPresent();
    }

    @DisplayName("쿠폰 실패로 결제 취소를 요청한 뒤 결제 취소(CANCELLED) 응답을 받으면, 주문이 CANCELLED가 되고 원래 PaymentOutbox는 COMPENSATED, 재고 해제 Outbox가 저장된다.")
    @Test
    void shouldCancelOrderAndReleaseStock_whenPaymentCancelledAfterCouponFailure() {
        // given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PAID);
        saveCouponOutbox(sagaId, SagaStatus.STARTED, OrderStatus.PENDING);
        PaymentOutbox originalPaymentOutbox = savePaymentOutbox(sagaId, SagaStatus.PROCESSING, OrderStatus.PAID);

        orderCouponService.rollback(getCouponFailureResponse(sagaId));

        // when
        orderPaymentService.rollback(getPaymentCancelledResponse(sagaId));

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(paymentOutboxRepository.findById(originalPaymentOutbox.getId()).orElseThrow().getSagaStatus())
                .isEqualTo(SagaStatus.COMPENSATED);
        assertThat(productOutboxRepository.findBySagaIdAndSagaStatus(sagaId, SagaStatus.COMPENSATED)).isPresent();
    }

    @DisplayName("CouponOutbox가 이미 FAILED로 처리되었으면, 같은 쿠폰 실패 응답을 다시 받아도 주문 상태를 바꾸지 않고 결제 취소 요청도 저장하지 않는다.")
    @Test
    void shouldSkipRollback_whenCouponOutboxIsAlreadyFailed() {
        // given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.PAID);
        saveCouponOutbox(sagaId, SagaStatus.FAILED, OrderStatus.CANCELLING);

        // when
        orderCouponService.rollback(getCouponFailureResponse(sagaId));

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);

        assertThat(paymentOutboxRepository
                .findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId, List.of(SagaStatus.FAILED)))
                .isEmpty();
    }

    @DisplayName("고객 취소로 주문이 이미 CANCELLING이면, 쿠폰 사용 실패 응답을 받아도 예외 없이 결제 취소 요청을 생략하고 CouponOutbox만 FAILED로 바꾼다.")
    @Test
    void shouldSkipPaymentCancelRequestWithoutException_whenOrderIsAlreadyCancelling() {
        // given
        UUID sagaId = UUID.randomUUID();
        saveOrder(OrderStatus.CANCELLING);
        CouponOutbox couponOutbox = saveCouponOutbox(sagaId, SagaStatus.STARTED, OrderStatus.PENDING);

        // when
        orderCouponService.rollback(getCouponFailureResponse(sagaId));

        // then
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);

        assertThat(couponOutboxRepository.findById(couponOutbox.getId()).orElseThrow().getSagaStatus())
                .isEqualTo(SagaStatus.FAILED);
        assertThat(paymentOutboxRepository
                .findByTypeAndSagaIdAndSagaStatusIn(ORDER_SAGA_NAME, sagaId, List.of(SagaStatus.FAILED)))
                .isEmpty();
    }

    private void saveOrder(OrderStatus orderStatus) {
        orderRepository.save(Order.builder()
                .id(orderId)
                .customerId(customerId)
                .orderStatus(orderStatus)
                .restaurantId(restaurantId)
                .address(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .price(new Money(new BigDecimal("25.00")))
                .items(List.of(orderItem))
                .couponIds(List.of(1L))
                .build());
    }

    private CouponOutbox saveCouponOutbox(UUID sagaId, SagaStatus sagaStatus, OrderStatus orderStatus) {
        return couponOutboxRepository.save(CouponOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("{}")
                .sagaStatus(sagaStatus)
                .orderStatus(orderStatus)
                .build());
    }

    private PaymentOutbox savePaymentOutbox(UUID sagaId, SagaStatus sagaStatus, OrderStatus orderStatus) {
        return paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("{}")
                .sagaStatus(sagaStatus)
                .orderStatus(orderStatus)
                .build());
    }

    private CouponResponse getCouponFailureResponse(UUID sagaId) {
        return CouponResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .sagaId(sagaId)
                .createdAt(Instant.now())
                .issuedCouponId(List.of(1L))
                .updatedCount(0)
                .failureMessages(couponFailureMessages)
                .build();
    }

    private PaymentResponse getPaymentCancelledResponse(UUID sagaId) {
        return PaymentResponse.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .paymentId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .price(new BigDecimal("25.00"))
                .createdAt(Instant.now())
                .paymentStatus(PaymentStatus.CANCELLED.name())
                .failureMessages(new ArrayList<>())
                .build();
    }

}
