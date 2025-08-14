package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.ApprovalOrderItem;
import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import com.orderingsystem.restaurant.application.outbox.order.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderApprovalServiceTest extends ApplicationTestSupport {

    @Autowired
    private OrderApprovalService orderApprovalService;

    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final BigDecimal productPrice = new BigDecimal("25.00");

    @AfterEach
    void tearDown() {
        orderApprovalRepository.deleteAllInBatch();
        orderOutboxRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        restaurantProductRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 승인에 성공한다.")
    @Test
    void approveRestaurantSuccessfully() {
        //given
        saveRestaurant();
        saveProduct();
        saveRestaurantProduct();

        ApprovalRequest request = getApprovalRequest();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.APPROVED);
    }

    @DisplayName("레스토랑이 active 상태가 아니라면 거절된다.")
    @Test
    void rejectOrder_whenRestaurantIsNotActive() throws JsonProcessingException {
        //given
        saveRestaurant(false);
        saveProduct();
        saveRestaurantProduct();
        ApprovalRequest request = getApprovalRequest();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("레스토랑이 주문을 받을 수 없는 상태입니다. Order Id : " + orderId));
    }

    @DisplayName("주문이 결제가 되지 않았다면 거절된다.")
    @Test
    void rejectOrder_whenPaymentIsNotCompleted() throws JsonProcessingException {
        //given
        saveRestaurant();
        saveProduct();
        saveRestaurantProduct();
        ApprovalRequest request = getApprovalRequest(RestaurantOrderStatus.PENDING);

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderId));
    }

    @DisplayName("상품이 1개일 경우 주문할 수 없는 상태라면 거절된다.")
    @Test
    void rejectOrder_whenSingleProductIsUnavailable() throws JsonProcessingException {
        //given
        saveRestaurant();
        saveProduct(false);
        saveRestaurantProduct();

        ApprovalRequest request = getApprovalRequest();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("상품 Id가 " + productId + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품이 2개일 경우 모두 주문할 수 없는 상태라면 거절된다.")
    @Test
    void rejectOrder_whenAllProductsAreUnavailable() throws JsonProcessingException {
        //given
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        saveRestaurant();
        saveProduct(productId1, false);
        saveProduct(productId2, false);
        saveRestaurantProduct(restaurantId, productId1);
        saveRestaurantProduct(restaurantId, productId2);

        ApprovalRequest request = ApprovalRequest.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .restaurantOrderStatus(RestaurantOrderStatus.PAID)
                .products(List.of(ApprovalOrderItem.builder()
                                .productId(productId1)
                                .quantity(1)
                                .build(),
                        ApprovalOrderItem.builder()
                                .productId(productId2)
                                .quantity(1)
                                .build()))
                .price(productPrice.multiply(new BigDecimal(2)))
                .createdAt(Instant.now())
                .build();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("상품 Id가 " + productId1 + "인 상품은 주문이 불가능한 상태입니다.",
                        "상품 Id가 " + productId2 + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품이 2개일 경우 하나라도 주문할 수 없는 상태라면 거절된다.")
    @Test
    void rejectOrder_whenAtLeastOneProductIsUnavailable() throws JsonProcessingException {
        //given
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        saveRestaurant();
        saveProduct(productId1, false);
        saveProduct(productId2, true);
        saveRestaurantProduct(restaurantId, productId1);
        saveRestaurantProduct(restaurantId, productId2);

        ApprovalRequest request = ApprovalRequest.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .restaurantOrderStatus(RestaurantOrderStatus.PAID)
                .products(List.of(ApprovalOrderItem.builder()
                                .productId(productId1)
                                .quantity(1)
                                .build(),
                        ApprovalOrderItem.builder()
                                .productId(productId2)
                                .quantity(1)
                                .build()))
                .price(productPrice.multiply(new BigDecimal(2)))
                .createdAt(Instant.now())
                .build();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("상품 Id가 " + productId1 + "인 상품은 주문이 불가능한 상태입니다."));
    }

    @DisplayName("상품의 금액과 주문 금액이 일치하지 않으면 거절된다.")
    @Test
    void rejectOrder_whenProductPriceDoesNotMatchOrderAmount() throws JsonProcessingException {
        //given
        saveRestaurant();
        saveProduct();
        saveRestaurantProduct();

        ApprovalRequest request = ApprovalRequest.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .restaurantOrderStatus(RestaurantOrderStatus.PAID)
                .products(List.of(ApprovalOrderItem.builder()
                        .productId(productId)
                        .quantity(1)
                        .build()))
                .price(productPrice.multiply(new BigDecimal(2)))
                .createdAt(Instant.now())
                .build();

        //when
        orderApprovalService.approveOrder(request);

        //then
        Optional<OrderApproval> orderApproval = orderApprovalRepository.findByOrderId(orderId);
        assertThat(orderApproval).isPresent();
        assertThat(orderApproval.get().getStatus()).isEqualTo(OrderApprovalStatus.REJECTED);

        OrderEventPayload orderEventPayload = getOrderEventPayload();
        assertThat(orderEventPayload.getFailureMessages()).isEqualTo(
                List.of("해당 주문의 총 금액이 올바르지 않습니다. Order Id : " + orderId));
    }

    @DisplayName("해당 주문을 이미 처리하였으면 중복해서 처리하지 않는다.")
    @Test
    void doNotProcessOrder_whenAlreadyHandled() {
        //given
        orderOutboxRepository.save(OrderOutbox.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .processedAt(ZonedDateTime.now())
                .type(ORDER_SAGA_NAME)
                .payload("payload")
                .outboxStatus(OutboxStatus.COMPLETED)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .build());

        saveRestaurant();
        saveProduct();
        saveRestaurantProduct();

        ApprovalRequest request = getApprovalRequest();

        //when
        orderApprovalService.approveOrder(request);

        //then
        assertThat(orderApprovalRepository.count()).isEqualTo(0);
    }

    @DisplayName("레스토랑 정보를 찾을 수 없으면 예외가 발생한다.")
    @Test
    void throwException_whenRestaurantNotFound() {
        //given
        saveProduct();

        ApprovalRequest request = getApprovalRequest();

        //when, then
        assertThatThrownBy(() -> orderApprovalService.approveOrder(request))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑을 찾을 수 없습니다.");
    }

    @DisplayName("해당 주문이 이미 승인된 상태로 저장되어 있으면, 중복해서 저장하지 않는다.")
    @Test
    void doNotSaveOrder_whenAlreadyApproved() {
        //given
        orderApprovalRepository.save(OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .restaurantId(restaurantId)
                .status(OrderApprovalStatus.APPROVED)
                .build());

        saveRestaurant();
        saveProduct();
        saveRestaurantProduct();
        ApprovalRequest request = getApprovalRequest();

        assertThat(orderApprovalRepository.count()).isEqualTo(1);

        //when
        orderApprovalService.approveOrder(request);

        //then
        assertThat(orderApprovalRepository.count()).isEqualTo(1);
    }

    @DisplayName("레스토랑에 연결된 제품이 없으면 예외가 발생한다.")
    @Test
    void throwException_whenRestaurantProductsMissing() {
        // given
        saveRestaurant();
        saveProduct();

        ApprovalRequest request = getApprovalRequest();

        // when, then
        assertThatThrownBy(() -> orderApprovalService.approveOrder(request))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑을 찾을 수 없습니다");
    }

    private void saveRestaurant(Boolean active) {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("restaurant")
                .active(active)
                .build());
    }

    private void saveRestaurant() {
        saveRestaurant(true);
    }

    private void saveProduct() {
        saveProduct(true);
    }

    private void saveProduct(boolean available) {
        saveProduct(productId, available);
    }

    private void saveProduct(UUID productId, boolean available) {
        productRepository.save(Product.builder()
                .productId(productId)
                .name("product")
                .price(new Money(productPrice))
                .available(available)
                .build());
    }

    private void saveRestaurantProduct() {
        saveRestaurantProduct(restaurantId, productId);
    }

    private void saveRestaurantProduct(UUID restaurantId, UUID productId) {
        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .restaurantId(restaurantId)
                .build());
    }

    private ApprovalRequest getApprovalRequest() {
        return getApprovalRequest(RestaurantOrderStatus.PAID);
    }

    private ApprovalRequest getApprovalRequest(RestaurantOrderStatus restaurantOrderStatus) {
        return ApprovalRequest.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .restaurantOrderStatus(restaurantOrderStatus)
                .products(List.of(ApprovalOrderItem.builder()
                        .productId(productId)
                        .quantity(1)
                        .build()))
                .price(productPrice)
                .createdAt(Instant.now())
                .build();
    }

    private OrderEventPayload getOrderEventPayload() throws JsonProcessingException {
        Optional<List<OrderOutbox>> orderOutbox = orderOutboxRepository.findByTypeAndOutboxStatus(ORDER_SAGA_NAME,
                OutboxStatus.STARTED);
        assertThat(orderOutbox).isPresent();
        String payload = orderOutbox.get().get(0).getPayload();
        return objectMapper.readValue(payload, OrderEventPayload.class);
    }

}
