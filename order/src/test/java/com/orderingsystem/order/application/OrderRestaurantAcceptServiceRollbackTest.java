package com.orderingsystem.order.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.RestaurantAcceptOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
class OrderRestaurantAcceptServiceRollbackTest {

    @Autowired
    private OrderRestaurantAcceptService orderRestaurantAcceptService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantAcceptOutboxRepository restaurantAcceptOutboxRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2025-08-05T15:06:00Z");
    private final UUID productId = UUID.randomUUID();
    private final UUID restaurantAcceptOutboxId = UUID.randomUUID();
    private final UUID paymentOutboxId = UUID.randomUUID();
    private final OrderItem orderItem = OrderItem.builder()
            .productId(productId)
            .build();

    @DisplayName("레스토랑에서 REJECT 되었고, 해당 주문이 PAID 상태라면 주문 취소를 시작한다.")
    @Test
    void rollbackOrder_whenRestaurantApprovedAndOrderIsPaid() {
        //given
        saveOrder(OrderStatus.PAID);
        saveOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        long beforePaymentOutboxCount = paymentOutboxRepository.count();
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        long afterPaymentOutboxCount = paymentOutboxRepository.count();
        Optional<RestaurantAcceptOutbox> restaurantAcceptOutbox = restaurantAcceptOutboxRepository.findById(
                restaurantAcceptOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(restaurantAcceptOutbox).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(restaurantAcceptOutbox.get().getSagaStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(restaurantAcceptOutbox.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(beforePaymentOutboxCount+1).isEqualTo(afterPaymentOutboxCount);
    }

    @DisplayName("주문이 이미 APPROVED 상태라면 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOrderStatusIsAPPROVED() {
        //given
        saveOrder(OrderStatus.APPROVED);
        saveOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("주문이 CANCELLED 상태라면 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOrderStatusIsCANCELLED() {
        //given
        saveOrder(OrderStatus.CANCELLED);
        saveOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("주문이 CANCELLING 상태라면 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOrderStatusIsCANCELLING() {
        //given
        saveOrder(OrderStatus.CANCELLING);
        saveOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("해당 주문에 대한 RestaurantAcceptOutbox에 대한 SAGA Status가 STARTED 상태라면 결제 처리가 되지 않은 상태이므로 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOutboxStatusIsSTARTED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("해당 주문에 대한 RestaurantAcceptOutbox에 대한 SAGA Status가 FAILED 상태라면 주문에 실패했으므로 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOutboxStatusIsFAILED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("해당 주문에 대한 RestaurantAcceptOutbox에 대한 SAGA Status가 SUCCEEDED 상태라면 이미 주문 로직이 완료된 상태이므로 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOutboxStatusIsSUCCEEDED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("해당 주문에 대한 RestaurantAcceptOutbox에 대한 SAGA Status가 COMPENSATING 상태라면 주문 실패로 인해 롤백 처리 중이므로 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOutboxStatusIsCOMPENSATING() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    @DisplayName("해당 주문에 대한 RestaurantAcceptOutbox에 대한 SAGA Status가 COMPENSATED 상태라면 롤백 처리를 완료했으므로 취소 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToRollbackOrder_whenOutboxStatusIsCOMPENSATED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantOrderDecisionResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantAcceptService.rollback(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> after = paymentOutboxRepository.findById(paymentOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(before).isPresent();
        assertThat(after).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(before.get().getId()).isEqualTo(after.get().getId());
        assertThat(before.get().getSagaStatus()).isEqualTo(after.get().getSagaStatus());
        assertThat(after.get().getSagaStatus()).isEqualTo(SagaStatus.PROCESSING);
    }

    private RestaurantOrderDecisionResponse getRestaurantApprovalResponse() {
        return RestaurantOrderDecisionResponse.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .createdAt(createdAt)
                .orderApprovalStatus(OrderApprovalStatus.REJECTED)
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

    private void saveOutbox() {
        restaurantAcceptOutboxRepository.save(RestaurantAcceptOutbox.builder()
                .id(restaurantAcceptOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(paymentOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
    }

    private void savePaymentOutbox() {
        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(paymentOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
    }
}
