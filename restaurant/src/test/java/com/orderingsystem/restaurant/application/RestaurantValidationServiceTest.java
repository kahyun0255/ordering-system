package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RestaurantValidationServiceTest extends ApplicationTestSupport {

    @Autowired
    private RestaurantValidationService restaurantValidationService;

    private static final UUID sagaId = UUID.randomUUID();
    private static final UUID restaurantId = UUID.randomUUID();
    private static final UUID productId1 = UUID.randomUUID();
    private static final BigDecimal product1Price = new BigDecimal("25.00");
    private static final UUID productId2 = UUID.randomUUID();
    private static final BigDecimal product2Price = new BigDecimal("20.00");

    private static final UUID productId3 = UUID.randomUUID();
    private static final BigDecimal product3Price = new BigDecimal("20.00");
    private static final UUID productId4 = UUID.randomUUID();
    private static final BigDecimal product4Price = new BigDecimal("20.00");
    private static final UUID restaurantId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
                .build());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId2)
                .name("ACTIVE 상태가 아닌 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build());

        productRepository.save(Product.builder()
                .productId(productId1)
                .name("상품1")
                .price(new Money(product1Price))
                .available(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .name("상품2")
                .price(new Money(product2Price))
                .available(true)
                .build());

        productRepository.save(Product.builder()
                .productId(productId3)
                .name("판매 불가 상품")
                .price(new Money(product3Price))
                .available(false)
                .build());

        productRepository.save(Product.builder()
                .productId(productId4)
                .name("레스토랑이 판매하지 않는 상품")
                .price(new Money(product4Price))
                .available(true)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId1)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId2)
                .build());

        restaurantProductRepository.save(RestaurantProduct.builder()
                .id(UUID.randomUUID())
                .restaurantId(restaurantId)
                .productId(productId3)
                .build());
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantProductRepository.deleteAllInBatch();
    }

    @DisplayName("주문 검증에 성공한다.")
    @Test
    void validateOrder_success() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId2)
                                .price(product2Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product2Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertDoesNotThrow(() -> restaurantValidationService.validate(restaurant, request));
    }

    @DisplayName("주문 상품이 null이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderItemsAreNull() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(null)
                .totalPrice(BigDecimal.ZERO)
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 상품이 비어있습니다.");
    }

    @DisplayName("주문 상품이 비어있으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderItemsAreEmpty() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of())
                .totalPrice(BigDecimal.ZERO)
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 상품이 비어있습니다.");
    }

    @DisplayName("존재하지 않는 상품을 주문하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductDoesNotExist() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(UUID.randomUUID())
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId2)
                                .price(product2Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product2Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문에 존재하지 않는 상품이 포함되어있습니다.");
    }

    @DisplayName("판매가 불가능한 상품을 주문하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductIsNotForSale() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId3)
                                .price(product3Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product3Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문에 판매 불가능한 상품이 포함되어 있습니다.");
    }

    @DisplayName("주문한 상품의 가격과 판매 가격이 다르면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductPriceMismatch() {
        //given
        BigDecimal price = new BigDecimal("300000.00");

        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId2)
                                .price(price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문을 요청한 상품의 가격과 판매 가격이 일치하지 않습니다.");
    }

    @DisplayName("총 주문 금액이 다르면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenTotalOrderAmountMismatch() {
        //given
        BigDecimal price = new BigDecimal("300000.00");

        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId2)
                                .price(product2Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("총 주문 금액이 일치하지 않습니다.");
    }

    @DisplayName("레스토랑이 판매하지 않는 상품을 주문하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderingProductNotSoldByRestaurant() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId4)
                                .price(product4Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product4Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 레스토랑이 판매하지 않는 상품이 있습니다.");
    }

    @DisplayName("레스토랑이 활성화 상태가 아니면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderingFromInactiveRestaurant() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId4)
                                .price(product4Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product4Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId2).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레스토랑이 주문 가능한 상태가 아닙니다.");
    }

    @DisplayName("주문 상품의 수량이 0개이면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderItemQuantityIsZero() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(0)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId4)
                                .price(product4Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product4Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 수량이 유효하지 않습니다.");
    }

    @DisplayName("주문 상품의 수량이 음수면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderItemQuantityIsNegative() {
        //given
        RestaurantValidationApplicationRequest request = RestaurantValidationApplicationRequest.builder()
                .sagaId(sagaId)
                .items(List.of(RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId1)
                                .price(product1Price)
                                .quantity(-1)
                                .build(),
                        RestaurantValidationApplicationRequest.Item.builder()
                                .productId(productId4)
                                .price(product4Price)
                                .quantity(2)
                                .build()))
                .totalPrice(product1Price.add(product4Price.multiply(BigDecimal.valueOf(2))))
                .build();

        Restaurant restaurant = restaurantRepository.findById(restaurantId).get();

        //when, then
        assertThatThrownBy(() -> restaurantValidationService.validate(restaurant, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 수량이 유효하지 않습니다.");
    }

}
