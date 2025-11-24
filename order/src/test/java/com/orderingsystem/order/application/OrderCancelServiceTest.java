package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentOutboxHelper paymentOutboxHelper;

    @Mock
    private OrderDataMapper orderDataMapper;

    @InjectMocks
    private OrderCancelService orderCancelService;

    private final UUID customerId = UUID.randomUUID();

    @DisplayName("주문이 존재하고, 해당 주문을 요청한 유저가 주문을 취소하면 주문 취소에 성공한다.")
    @Test
    void shouldCancelOrder_whenUserIsOrdererAndOrderExists() {
        //given
        UUID trackingId = UUID.randomUUID();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(customerId)
                .trackingId(trackingId)
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        OrderPaymentEventPayload payload = OrderPaymentEventPayload.builder().build();

        given(orderRepository.findByTrackingId(trackingId)).willReturn(Optional.of(order));
        given(orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(any(), any())).willReturn(
                payload);

        //when
        orderCancelService.cancelOrder(trackingId, customerId);

        //then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);

        verify(paymentOutboxHelper).savePaymentOutboxMessage(
                eq(payload),
                eq(OrderStatus.CANCELLING),
                eq(SagaStatus.COMPENSATING),
                any(UUID.class)
        );

        verify(orderRepository).findByTrackingId(trackingId);
    }

    @DisplayName("trackingId에 해당하는 주문이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowOrderNotFoundException_whenTrackingIdIsInvalid() {
        //given
        UUID trackingId = UUID.randomUUID();

        given(orderRepository.findByTrackingId(trackingId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderCancelService.cancelOrder(trackingId, customerId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문 내역을 찾을 수 없습니다.");

        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    @DisplayName("주문을 요청한 유저와 주문 취소를 요청한 유저가 다르면 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserIsNotOrderer() {
        //given
        UUID trackingId = UUID.randomUUID();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(OrderStatus.ACCEPTED)
                .customerId(UUID.randomUUID())
                .trackingId(trackingId)
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        given(orderRepository.findByTrackingId(trackingId)).willReturn(Optional.of(order));

        //when, then
        assertThatThrownBy(() -> orderCancelService.cancelOrder(trackingId, customerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("주문을 취소할 권한이 없습니다.");

        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    @DisplayName("주문을 한 유저가 주문 취소를 요청할 때 APPROVED, CANCELLING, CANCELLED 상태라면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 주문 상태 : {0}")
    @MethodSource("provideNonCancellableOrderStatuses")
    void shouldReturn400_whenCancellingNonCancellableOrder(String status, OrderStatus orderStatus) throws Exception {
        UUID trackingId = UUID.randomUUID();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .price(new Money(BigDecimal.valueOf(1000)))
                .address(UUID.randomUUID())
                .orderStatus(orderStatus)
                .customerId(customerId)
                .trackingId(trackingId)
                .restaurantId(UUID.randomUUID())
                .items(List.of(OrderItem.builder()
                        .subTotal(new Money(BigDecimal.valueOf(1000)))
                        .quantity(10)
                        .productId(UUID.randomUUID())
                        .price(new Money(BigDecimal.valueOf(1000)))
                        .build()))
                .build();

        given(orderRepository.findByTrackingId(trackingId)).willReturn(Optional.of(order));

        //when, then
        assertThatThrownBy(() -> orderCancelService.cancelOrder(trackingId, customerId))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");

        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    private static Stream<Arguments> provideNonCancellableOrderStatuses() {
        return Stream.of(
                Arguments.of("APPROVED", OrderStatus.APPROVED),
                Arguments.of("CANCELLING", OrderStatus.CANCELLING),
                Arguments.of("CANCELLED", OrderStatus.CANCELLED)
        );
    }

}
