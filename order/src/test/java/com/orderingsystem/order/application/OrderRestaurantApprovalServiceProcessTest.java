package com.orderingsystem.order.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.Product;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import com.orderingsystem.order.domain.repository.outbox.RestaurantApprovalOutboxRepository;
import com.orderingsystem.outbox.OutboxStatus;
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
class OrderRestaurantApprovalServiceProcessTest {

    @Autowired
    private OrderRestaurantApprovalService orderRestaurantApprovalService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantApprovalOutboxRepository restaurantApprovalOutboxRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final Instant createdAt = Instant.parse("2025-08-05T15:06:00Z");
    private final UUID productId = UUID.randomUUID();
    private final UUID restaurantApprovalOutboxId = UUID.randomUUID();
    private final UUID paymentOutboxId = UUID.randomUUID();
    private final OrderItem orderItem = OrderItem.builder()
            .productId(productId)
            .product(Product.builder()
                    .productId(productId)
                    .name("product1")
                    .price(new Money(new BigDecimal("25.00")))
                    .build())
            .build();

    @DisplayName("레스토랑에서 APPROVED 되었고, 해당 주문이 PAID 상태라면 주문을 승인 처리 한다.")
    @Test
    void approveOrder_whenRestaurantApprovedAndOrderIsPaid() {
        //given
        saveOrder(OrderStatus.PAID);
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        orderRestaurantApprovalService.process(request);
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<PaymentOutbox> paymentOutbox = paymentOutboxRepository.findById(paymentOutboxId);
        Optional<RestaurantApprovalOutbox> restaurantApprovalOutbox = restaurantApprovalOutboxRepository.findById(
                restaurantApprovalOutboxId);

        //then
        assertThat(order).isPresent();
        assertThat(paymentOutbox).isPresent();
        assertThat(restaurantApprovalOutbox).isPresent();
        assertThat(order.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(paymentOutbox.get().getSagaStatus()).isEqualTo(SagaStatus.SUCCEEDED);
        assertThat(restaurantApprovalOutbox.get().getSagaStatus()).isEqualTo(SagaStatus.SUCCEEDED);
        assertThat(paymentOutbox.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(restaurantApprovalOutbox.get().getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @DisplayName("해당 주문에 대한 정보를 찾을 수 없으면 예외가 발생한다.")
    @Test
    void notFoundOrder() {
        //given
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when, then
        assertThatThrownBy(()->orderRestaurantApprovalService.process(request))
                .isInstanceOf(OrderNotFoundException.class)
                        .hasMessage("주문 정보를 찾을 수 없습니다. Order Id : " + orderId);
    }

    @DisplayName("PaymentOutbox에서 SagaStatus가 PROCESSING인 Outbox Message 정보를 찾을 수 없으면 예외가 발생한다.")
    @Test
    void notFoundPaymentOutboxMessage() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when, then
        assertThatThrownBy(()->orderRestaurantApprovalService.process(request))
                .isInstanceOf(OrderApplicationException.class)
                .hasMessage("SagaStatus가 " + SagaStatus.PROCESSING.name() + " 상태인 PaymentOutbox를 찾지 못했습니다.");
    }

    @DisplayName("주문이 PENDING 상태라면 예외가 발생하며 주문 승인에 실패한다.")
    @Test
    void failToApproveOrder_whenOrderStatusIsPending() {
        //given
        saveOrder(OrderStatus.PENDING);
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when, then
        assertThatThrownBy(() -> orderRestaurantApprovalService.process(request))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("승인할 수 없는 주문 상태입니다.");
    }

    @DisplayName("주문이 이미 APPROVED 상태라면 승인 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOrderStatusIsAPPROVED() {
        //given
        saveOrder(OrderStatus.APPROVED);
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("주문이 CANCELLED 상태라면 승인 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOrderStatusIsCANCELLED() {
        //given
        saveOrder(OrderStatus.CANCELLED);
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("주문이 CANCELLING 상태라면 승인 처리 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOrderStatusIsCANCELLING() {
        //given
        saveOrder(OrderStatus.CANCELLING);
        saveOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("해당 주문에 대한 RestaurantApprovalOutbox에 대한 SAGA Status가 STARTED 상태라면 결제 처리가 되지 않은 상태이므로 주문 승인 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOutboxStatusIsSTARTED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.STARTED)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("해당 주문에 대한 RestaurantApprovalOutbox에 대한 SAGA Status가 FAILED 상태라면 주문에 실패했으므로 주문 승인 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOutboxStatusIsFAILED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("해당 주문에 대한 RestaurantApprovalOutbox에 대한 SAGA Status가 SUCCEEDED 상태라면 이미 주문 로직이 완료된 상태이므로 주문 승인 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOutboxStatusIsSUCCEEDED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("해당 주문에 대한 RestaurantApprovalOutbox에 대한 SAGA Status가 COMPENSATING 상태라면 주문 실패로 인해 롤백 처리 중이므로 주문 승인 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOutboxStatusIsCOMPENSATING() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    @DisplayName("해당 주문에 대한 RestaurantApprovalOutbox에 대한 SAGA Status가 COMPENSATED 상태라면 롤백 처리를 완료했으므로 주문 승인 로직을 추가적으로 진행하지 않는다.")
    @Test
    void failToApproveOrder_whenOutboxStatusIsCOMPENSATED() {
        //given
        saveOrder(OrderStatus.PAID);
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.FAILED)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
        savePaymentOutbox();
        RestaurantApprovalResponse request = getRestaurantApprovalResponse();

        //when
        Optional<PaymentOutbox> before = paymentOutboxRepository.findById(paymentOutboxId);
        orderRestaurantApprovalService.process(request);
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

    private RestaurantApprovalResponse getRestaurantApprovalResponse() {
        return RestaurantApprovalResponse.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .createdAt(createdAt)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
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
        restaurantApprovalOutboxRepository.save(RestaurantApprovalOutbox.builder()
                .id(restaurantApprovalOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());

        paymentOutboxRepository.save(PaymentOutbox.builder()
                .id(paymentOutboxId)
                .sagaId(sagaId)
                .sagaStatus(SagaStatus.PROCESSING)
                .outboxStatus(OutboxStatus.STARTED)
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
                .outboxStatus(OutboxStatus.STARTED)
                .orderStatus(OrderStatus.PENDING)
                .type(ORDER_SAGA_NAME)
                .payload("")
                .build());
    }
}
