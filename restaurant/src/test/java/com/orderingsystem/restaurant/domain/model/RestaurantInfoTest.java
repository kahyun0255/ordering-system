package com.orderingsystem.restaurant.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantInfoTest {

    private final UUID sagaId = UUID.randomUUID();

    @DisplayName("주문이 유효하면 검증에 성공한다.")
    @Test
    void validateOrderSuccessfully() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(UUID.randomUUID())
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(true)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEmpty();
    }

    @DisplayName("주문 상태가 PENDING이면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenOrderStatusIsPending() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PENDING)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(UUID.randomUUID())
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(true)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderId));
    }

    @DisplayName("주문 상태가 APPROVED이면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenOrderStatusIsApproved() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.APPROVED)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(UUID.randomUUID())
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(true)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderId));
    }

    @DisplayName("주문 상태가 CANCELLING이면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenOrderStatusIsCancelling() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.CANCELLING)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(UUID.randomUUID())
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(true)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderId));
    }

    @DisplayName("주문 상태가 CANCELLED이면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenOrderStatusIsCancelled() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.CANCELLED)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(UUID.randomUUID())
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(true)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderId));
    }

    @DisplayName("상품이 1개일 때, 상품 상태가 준비되지 않았다면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenSingleProductIsNotAvailable() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("25.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                .product(Product.builder()
                                        .productId(productId)
                                        .name("productName")
                                        .price(new Money(new BigDecimal("25.00")))
                                        .available(false)
                                        .build())
                                .quantity(1)
                                .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("상품 Id가 " + productId + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품이 2개일 때, 하나라도 준비되지 않은 상태라면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenAnyProductIsNotAvailable() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("50.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId1)
                                                .name("productName1")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(false)
                                                .build())
                                        .quantity(1)
                                        .build(),
                                OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId2)
                                                .name("productName2")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(true)
                                                .build())
                                        .quantity(1)
                                        .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("상품 Id가 " + productId1 + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품이 2개일 때, 전부 준비되지 않은 상태라면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenAllProductsAreNotAvailable() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("50.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId1)
                                                .name("productName1")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(false)
                                                .build())
                                        .quantity(1)
                                        .build(),
                                OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId2)
                                                .name("productName2")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(false)
                                                .build())
                                        .quantity(1)
                                        .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("상품 Id가 " + productId1 + "인 상품은 주문이 불가능한 상태입니다.",
                "상품 Id가 " + productId2 + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품의 개별 가격과 합이 일치하지 않으면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenItemTotalDoesNotMatchOrderPrice() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.ACTIVE)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("3.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId1)
                                                .name("productName1")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(true)
                                                .build())
                                        .quantity(1)
                                        .build(),
                                OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId2)
                                                .name("productName2")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(true)
                                                .build())
                                        .quantity(1)
                                        .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("해당 주문의 총 금액이 올바르지 않습니다. Order Id : " + orderId));
    }

    @DisplayName("레스토랑이 준비되지 않은 상태면 주문 검증에 실패한다.")
    @Test
    void failToValidateOrder_whenRestaurantIsNotAvailable() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .restaurantName("restaurantName")
                .status(RestaurantStatus.PRE_OPEN)
                .orderApproval(OrderApproval.builder()
                        .id(UUID.randomUUID())
                        .restaurantId(restaurantId)
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderId)
                        .orderStatus(OrderStatus.PAID)
                        .totalAmount(new Money(new BigDecimal("50.00")))
                        .orderProducts(List.of(OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId1)
                                                .name("productName1")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(true)
                                                .build())
                                        .quantity(1)
                                        .build(),
                                OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(productId2)
                                                .name("productName2")
                                                .price(new Money(new BigDecimal("25.00")))
                                                .available(true)
                                                .build())
                                        .quantity(1)
                                        .build()))
                        .build())
                .build();

        //when
        restaurantInfo.validateOrder(failureMessages);

        //then
        assertThat(failureMessages).isEqualTo(List.of("레스토랑이 주문을 받을 수 없는 상태입니다. Order Id : " + orderId));
    }

    @DisplayName("OrderApprovalStatus를 받아 OrderApproval을 업데이트 할 수 있다.")
    @Test
    void updateOrderApproval() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderApprovalId = UUID.randomUUID();
        UUID orderDetailOrderId = UUID.randomUUID();
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .orderApproval(OrderApproval.builder()
                        .id(orderApprovalId)
                        .restaurantId(UUID.randomUUID())
                        .status(OrderApprovalStatus.APPROVED)
                        .orderId(orderId)
                        .build())
                .orderDetail(OrderDetail.builder()
                        .orderId(orderDetailOrderId)
                        .build())
                .build();

        //when
        restaurantInfo.rejectOrder(failureMessages, sagaId);

        //then
        assertThat(restaurantInfo.getOrderApproval().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);
        assertThat(restaurantInfo.getOrderApproval().getId()).isNotEqualTo(orderApprovalId);
        assertThat(restaurantInfo.getOrderApproval().getOrderId()).isEqualTo(orderDetailOrderId);
        assertThat(restaurantInfo.getOrderApproval().getRestaurantId()).isEqualTo(restaurantId);
    }

}
