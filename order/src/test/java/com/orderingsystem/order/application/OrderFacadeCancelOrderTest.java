package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderFacadeCancelOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCancelService orderCancelService;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerRepository.save(Customer.builder()
                .id(customerId)
                .name("유저")
                .build());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        paymentOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("주문을 한 유저가 주문 취소를 요청하면 PENDING, PAID, ACCEPTED 상태라면 주문 취소를 접수한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideCancellableOrderStatuses")
    void shouldCancelOrder_whenUserIsOrdererAndStatusIsCancellable(String status, OrderStatus orderStatus) {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(orderStatus)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        //when
        orderFacade.cancelOrder(order.getTrackingId(), customerId);

        //then
        Optional<Order> afterOrder = orderRepository.findById(order.getId());
        assertThat(afterOrder).isPresent();
        assertThat(afterOrder.get().getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);

        assertThat(paymentOutboxRepository.count()).isOne();
    }

    private static Stream<Arguments> provideCancellableOrderStatuses() {
        return Stream.of(
                Arguments.of("PENDING", OrderStatus.PENDING),
                Arguments.of("PAID", OrderStatus.PAID),
                Arguments.of("ACCEPTED", OrderStatus.ACCEPTED)
        );
    }

    @DisplayName("주문을 한 유저가 주문 취소를 요청할 때 APPROVED, CANCELLING, CANCELLED 상태라면 주문 취소를 접수할 수 없고, 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideNonCancellableOrderStatuses")
    void shouldThrowOrderCancellationNotAllowedException_whenOrderStatusIsNotCancellable(String status,
                                                                                         OrderStatus orderStatus) {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(orderStatus)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        //when, then
        assertThatThrownBy(() -> orderFacade.cancelOrder(order.getTrackingId(), customerId))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");
    }

    private static Stream<Arguments> provideNonCancellableOrderStatuses() {
        return Stream.of(
                Arguments.of("APPROVED", OrderStatus.APPROVED),
                Arguments.of("CANCELLING", OrderStatus.CANCELLING),
                Arguments.of("CANCELLED", OrderStatus.CANCELLED)
        );
    }

    @DisplayName("주문 취소를 요청한 유저가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCancellingOrderByNonexistentUser() {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(customerId)
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        //when, then
        assertThatThrownBy(() -> orderFacade.cancelOrder(order.getTrackingId(), UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문자를 찾을 수 없습니다.");
    }

    @DisplayName("주문을 신청한 유저와 주문 취소를 요청한 유저가 다르면 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserIsNotOrderer() {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        orderRepository.save(order);

        //when, then
        assertThatThrownBy(() -> orderFacade.cancelOrder(order.getTrackingId(), customerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("주문을 취소할 권한이 없습니다.");
    }

    @DisplayName("trackingId에 해당하는 주문이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowOrderNotFoundException_whenTrackingIdIsInvalid() {
        //when, then
        assertThatThrownBy(() -> orderFacade.cancelOrder(UUID.randomUUID(), customerId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문 내역을 찾을 수 없습니다.");
    }

}
