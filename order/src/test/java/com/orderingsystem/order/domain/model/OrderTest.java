package com.orderingsystem.order.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @DisplayName("동일한 값의 Order는 id가 동일해야 한다.")
    @Test
    void sameOrderValues_shouldHaveSameId() {
        //given
        UUID id = UUID.randomUUID();

        Order order1 = Order.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .orderStatus(OrderStatus.PENDING)
                .price(new Money(new BigDecimal(10000)))
                .failureMessages(null)
                .address(UUID.randomUUID())
                .items(List.of())
                .build();

        Order order2 = Order.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .trackingId(UUID.randomUUID())
                .orderStatus(OrderStatus.PENDING)
                .price(new Money(new BigDecimal(20000)))
                .failureMessages(null)
                .address(UUID.randomUUID())
                .items(List.of())
                .build();

        //then
        assertThat(order1).isEqualTo(order2);
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }

    @DisplayName("다른 값이 모두 동일하더라도 id가 다르면 다른 Order 객체이다.")
    @Test
    void differentId_shouldCreateDifferentOrderObjects() {
        //given
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID trackingId = UUID.randomUUID();
        UUID address = UUID.randomUUID();

        Order order1 = Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .restaurantId(restaurantId)
                .trackingId(trackingId)
                .orderStatus(OrderStatus.PENDING)
                .price(new Money(new BigDecimal(10000)))
                .failureMessages(null)
                .address(address)
                .items(List.of())
                .build();

        Order order2 = Order.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .restaurantId(restaurantId)
                .trackingId(trackingId)
                .orderStatus(OrderStatus.PENDING)
                .price(new Money(new BigDecimal(10000)))
                .failureMessages(null)
                .address(address)
                .items(List.of())
                .build();

        //then
        assertThat(order1).isNotEqualTo(order2);
        assertThat(order1.hashCode()).isNotEqualTo(order2.hashCode());
    }

    @DisplayName("주문을 초기화 한다.")
    @Test
    void initializeOrder() {
        //given
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID address = UUID.randomUUID();

        UUID productId = UUID.randomUUID();
        Money price = new Money(new BigDecimal("20.00"));
        int quantity = 1;
        Money subTotal = price.multiply(quantity);

        Product product = Product.builder()
                .productId(productId)
                .name("itemName")
                .price(price)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .price(price)
                .quantity(quantity)
                .subTotal(subTotal)
                .product(product)
                .build();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(address)
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(orderItem))
                .build();

        //when
        order.initializeOrder();

        //then
        assertThat(order.getId()).isNotNull();
        assertThat(order.getTrackingId()).isNotNull();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getRestaurantId()).isEqualTo(restaurantId);
    }

    @DisplayName("주문을 생성할 수 있을지 검증한다.")
    @Test
    void validateOrder() {
        //given
        Order order = getOrder();

        //when, then
        assertDoesNotThrow(() -> order.validateOrder(new ArrayList<>()));
    }

    @DisplayName("OrderStatus가 null이 아닌 경우, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenOrderStatusIsNotNull() {
        //given
        Order order = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(getOrderItem()))
                .orderStatus(OrderStatus.PENDING)
                .build();

        //when, then
        assertThatThrownBy(() -> order.validateOrder(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문이 초기화될 수 없는 상태입니다.(이미 생성된 주문일 수 있습니다.)");
    }

    @DisplayName("id가 null이 아닌 경우, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenIdIsNotNull() {
        //given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(getOrderItem()))
                .build();

        //when, then
        assertThatThrownBy(() -> order.validateOrder(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문이 초기화될 수 없는 상태입니다.(이미 생성된 주문일 수 있습니다.)");
    }

    @DisplayName("price가 null이면, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenPriceIsNull() {
        //given
        Order order = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(null)
                .build();

        List<String> failureMessages = new ArrayList<>();

        //when
        order.validateOrder(failureMessages);

        //then
        assertThat(failureMessages.get(0)).isEqualTo("총 주문 금액은 0보다 커야합니다.");
    }

    @DisplayName("주문 금액이 0이면, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenPriceIsZero() {
        //given
        Order order = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("0.00")))
                .build();

        List<String> failureMessages = new ArrayList<>();

        //when
        order.validateOrder(failureMessages);

        //then
        assertThat(failureMessages.get(0)).isEqualTo("총 주문 금액은 0보다 커야합니다.");
    }

    @DisplayName("주문 금액이 0보다 작으면, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenPriceIsNegative() {
        //given
        Order order = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("-1.00")))
                .build();

        List<String> failureMessages = new ArrayList<>();

        //when
        order.validateOrder(failureMessages);

        //then
        assertThat(failureMessages.get(0)).isEqualTo("총 주문 금액은 0보다 커야합니다.");
    }

    @DisplayName("총 주문 금액과 개별 항목들의 합계가 일치하지 않으면, 주문 초기화 검증에 실패한다.")
    @Test
    void failToInitializeOrder_whenTotalPriceDoesNotMatchItemSum() {
        //given
        Order order = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .build();

        List<String> failureMessages = new ArrayList<>();

        //when
        order.validateOrder(failureMessages);

        //then
        assertThat(failureMessages.get(0)).isEqualTo(
                "총 주문 금액 : 2.00 개별 항목들의 합계 : 20.00. 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다.");
    }

    @DisplayName("Order Status가 PENDING 상태면 PAID 상태로 갱신할 수 있다.")
    @Test
    void updateOrderStatusToPaid_whenOrderStatusIsPending() {
        //given
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID address = UUID.randomUUID();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(address)
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PENDING)
                .build();

        //when
        order.pay();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
    }

    @DisplayName("Order Status가 PENDING 상태가 아니면 PAID 상태로 갱신할 수 없다.")
    @Test
    void failToUpdateOrderStatusToPaid_whenOrderStatusIsNotPending() {
        //given
        Order order1 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.APPROVED)
                .build();

        Order order2 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PAID)
                .build();

        Order order3 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLING)
                .build();

        Order order4 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        //when, then
        assertThatThrownBy(order1::pay)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("결제를 진행할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order2::pay)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("결제를 진행할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order3::pay)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("결제를 진행할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order4::pay)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("결제를 진행할 수 없는 주문 상태입니다.");
    }

    @DisplayName("Order Status가 PAID 상태면 APPROVED 상태로 갱신할 수 있다.")
    @Test
    void updateOrderStatusToApproved_whenOrderStatusIsPaid() {
        //given
        UUID customerId = UUID.randomUUID();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PAID)
                .build();

        //when
        order.approve();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
    }

    @DisplayName("Order Status가 PAID 상태가 아니면 APPROVED 상태로 갱신할 수 없다.")
    @Test
    void failToUpdateOrderStatusToApproved_whenOrderStatusIsNotPaid() {
        //given
        Order order1 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.APPROVED)
                .build();

        Order order2 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order order3 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLING)
                .build();

        Order order4 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        //when, then
        assertThatThrownBy(order1::approve)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("승인할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order2::approve)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("승인할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order3::approve)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("승인할 수 없는 주문 상태입니다.");

        assertThatThrownBy(order4::approve)
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("승인할 수 없는 주문 상태입니다.");
    }

    @DisplayName("Order Status가 PAID 상태면 CANCELLING 상태로 갱신할 수 있다.")
    @Test
    void updateOrderStatusToCancelling_whenOrderStatusIsPaid() {
        //given
        UUID customerId = UUID.randomUUID();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PAID)
                .build();

        //when
        order.initCancel(new ArrayList<>());

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
    }

    @DisplayName("Order Status가 PAID 상태가 아니면 CANCELLING 상태로 갱신할 수 없다.")
    @Test
    void failToUpdateOrderStatusToCancelling_whenOrderStatusIsNotPaid() {
        //given
        Order order1 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.APPROVED)
                .build();

        Order order2 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order order3 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLING)
                .build();

        Order order4 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        //when, then
        assertThatThrownBy(() -> order1.initCancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");

        assertThatThrownBy(() -> order2.initCancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");

        assertThatThrownBy(() -> order3.initCancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");

        assertThatThrownBy(() -> order4.initCancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소할 수 없는 상태입니다.");
    }

    @DisplayName("Order Status가 CANCELLING이거나 PENDING 상태면 CANCELLED 상태로 갱신할 수 있다.")
    @Test
    void updateOrderStatusToCancelled_whenOrderStatusIsCancellingOrPending() {
        //given
        UUID customerId = UUID.randomUUID();

        Order order1 = Order.builder()
                .customerId(customerId)
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLING)
                .build();

        Order order2 = Order.builder()
                .customerId(customerId)
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PENDING)
                .build();

        //when
        order1.cancel(new ArrayList<>());
        order2.cancel(new ArrayList<>());

        // then
        assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order1.getCustomerId()).isEqualTo(customerId);

        assertThat(order2.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order2.getCustomerId()).isEqualTo(customerId);
    }

    @DisplayName("Order Status가 CANCELLING이거나 PENDING 상태가 아니면 CANCELLED 상태로 갱신할 수 없다.")
    @Test
    void failToUpdateOrderStatusToCancelled_whenOrderStatusIsNotCancellingOrPending() {
        //given
        Order order1 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.APPROVED)
                .build();

        Order order2 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PAID)
                .build();

        Order order3 = Order.builder()
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        //when, then
        assertThatThrownBy(() -> order1.cancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소 완료할 수 없는 상태입니다.");

        assertThatThrownBy(() -> order2.cancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소 완료할 수 없는 상태입니다.");

        assertThatThrownBy(() -> order3.cancel(new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문을 취소 완료할 수 없는 상태입니다.");
    }

    @DisplayName("String 형태의 failureMessages를 List 형태로 받을 수 있다.")
    @Test
    void convertFailureMessagesStringToList() {
        //given
        UUID customerId = UUID.randomUUID();

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(UUID.randomUUID())
                .address(UUID.randomUUID())
                .items(List.of(getOrderItem()))
                .price(new Money(new BigDecimal("2.00")))
                .orderStatus(OrderStatus.PAID)
                .failureMessages("주문 검증에 실패했습니다.,잔액이 부족합니다.")
                .build();

        //when
        List<String> failureMessageList = order.getFailureMessageList();
        List<String> failureMessages = List.of("주문 검증에 실패했습니다.", "잔액이 부족합니다.");

        // then
        assertThat(failureMessageList).isEqualTo(failureMessages);
    }

    private static Order getOrder() {
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID address = UUID.randomUUID();

        UUID productId = UUID.randomUUID();
        Money price = new Money(new BigDecimal("20.00"));
        int quantity = 1;
        Money subTotal = price.multiply(quantity);

        Product product = Product.builder()
                .productId(productId)
                .name("itemName")
                .price(price)
                .available(true)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(productId)
                .price(price)
                .quantity(quantity)
                .subTotal(subTotal)
                .product(product)
                .build();

        return Order.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(address)
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of(orderItem))
                .build();
    }

    private static OrderItem getOrderItem() {
        UUID productId = UUID.randomUUID();
        Money price = new Money(new BigDecimal("20.00"));
        int quantity = 1;
        Money subTotal = price.multiply(quantity);

        Product product = Product.builder()
                .productId(productId)
                .name("itemName")
                .price(price)
                .available(true)
                .build();

        return OrderItem.builder()
                .productId(productId)
                .price(price)
                .quantity(quantity)
                .subTotal(subTotal)
                .product(product)
                .build();
    }

}
