package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.application.dto.ProductInfo;
import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private RestaurantApi restaurantApi;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        customerRepository.deleteAllInBatch();
    }

    @BeforeEach
    void before() {
        customerRepository.save(Customer.builder()
                .id(customerId)
                .name("user1")
                .build());
    }

    private final UUID customerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();

    @DisplayName("주문 물품이 1개인 경우, 주문 생성에 성공한다.")
    @Test
    void createOrder_withOneProduct() {
        //given
        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(true)
                        .products(List.of(ProductInfo.builder()
                                .productId(productId1)
                                .name("product1")
                                .price(new BigDecimal("50.00"))
                                .available(true)
                                .build()))
                        .build());

        //when
        CreateOrderResponse response = orderService.createOrder(request);

        //then
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getMessage()).isEqualTo("주문이 성공적으로 생성되었습니다.");
    }

    @DisplayName("주문 물품이 2개인 경우, 주문 생성에 성공한다.")
    @Test
    void createOrder_withTwoProducts() {
        //given
        CreateOrderApplicationRequest request = getTwoProductCreateOrderApplicationRequest();

        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1, productId2)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(true)
                        .products(List.of(ProductInfo.builder()
                                        .productId(productId1)
                                        .name("product1")
                                        .price(new BigDecimal("50.00"))
                                        .available(true)
                                        .build(),
                                ProductInfo.builder()
                                        .productId(productId2)
                                        .name("product2")
                                        .price(new BigDecimal("25.00"))
                                        .available(true)
                                        .build()))
                        .build());

        //when
        CreateOrderResponse response = orderService.createOrder(request);

        //then
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getMessage()).isEqualTo("주문이 성공적으로 생성되었습니다.");
    }

    @DisplayName("상품이 1개일 때 상품 가격이 일치하지 않을 경우, 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenProductPricesAreMismatched_withOneProduct() {
        //given
        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(true)
                        .products(List.of(ProductInfo.builder()
                                .productId(productId1)
                                .name("product1")
                                .price(new BigDecimal("25.00"))
                                .available(true)
                                .build()))
                        .build());

        //when
        CreateOrderResponse orderResponse = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(orderResponse.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo("상품 : " + productId1 + "의 항목 가격 : 25.00이 유효하지 않습니다.");
        assertThat(orderResponse.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("상품이 2개 일 때 하나라도 상품 가격이 일치하지 않을 경우, 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenProductPricesAreMismatched_withTwoProducts() {
        //given
        CreateOrderApplicationRequest request = getTwoProductCreateOrderApplicationRequest();

        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1, productId2)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(true)
                        .products(List.of(ProductInfo.builder()
                                        .productId(productId1)
                                        .name("product1")
                                        .price(new BigDecimal("20.00"))
                                        .available(true)
                                        .build(),
                                ProductInfo.builder()
                                        .productId(productId2)
                                        .name("product2")
                                        .price(new BigDecimal("25.00"))
                                        .available(true)
                                        .build()))
                        .build());

        //when
        CreateOrderResponse createdOrder = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(createdOrder.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo("상품 : " + productId1 + "의 항목 가격 : 20.00이 유효하지 않습니다.");
        assertThat(createdOrder.getOrderStatus()).isEqualByComparingTo(OrderStatus.CANCELLED);
    }

    @DisplayName("주문자가 존재하지 않을 경우, 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenCustomerNotFound() {
        //given
        UUID notCustomerId = UUID.randomUUID();

        CreateOrderApplicationRequest request = CreateOrderApplicationRequest.builder()
                .customerId(notCustomerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(new BigDecimal("100.00"))
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(new BigDecimal("25.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build()))
                .build();

        restaurantApi();

        //when, then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문자를 찾을 수 없습니다. Customer Id : " + notCustomerId);
    }

    @DisplayName("레스토랑이 활성화 상태가 아니라면 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenNotActiveRestaurant() {
        //given
        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(false)
                        .products(List.of(ProductInfo.builder()
                                .productId(productId1)
                                .name("product1")
                                .price(new BigDecimal("50.00"))
                                .available(true)
                                .build()))
                        .build());

        //when
        CreateOrderResponse createdOrder = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(createdOrder.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo("restaurant Id : " + restaurantId + " active 상태가 아닙니다.");
        assertThat(createdOrder.getOrderStatus()).isEqualByComparingTo(OrderStatus.CANCELLED);
    }

    @DisplayName("개별 항목들의 총 합이 전체 총합과 일치하지 않으면 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenTotalPriceDoesNotMatchItemSum() {
        //given
        CreateOrderApplicationRequest request = CreateOrderApplicationRequest.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(new BigDecimal("20000.00"))
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(new BigDecimal("25.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build()))
                .build();

        restaurantApi();

        //when
        CreateOrderResponse createdOrder = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(createdOrder.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo(
                "총 주문 금액 : 20000.00 개별 항목들의 합계 : 100.00. 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다.");
        assertThat(createdOrder.getOrderStatus()).isEqualByComparingTo(OrderStatus.CANCELLED);
    }

    private CreateOrderApplicationRequest getOneProductCreateOrderApplicationRequest() {
        return CreateOrderApplicationRequest.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(new BigDecimal("50.00"))
                .items(List.of(OrderItemApplicationRequest.builder()
                        .productId(productId1)
                        .quantity(1)
                        .price(new BigDecimal("50.00"))
                        .subTotal(new BigDecimal("50.00"))
                        .build()))
                .build();
    }

    private CreateOrderApplicationRequest getTwoProductCreateOrderApplicationRequest() {
        return CreateOrderApplicationRequest.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(new BigDecimal("100.00"))
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(new BigDecimal("50.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(new BigDecimal("25.00"))
                                .subTotal(new BigDecimal("50.00"))
                                .build()))
                .build();
    }

    private void restaurantApi() {
        given(restaurantApi.getRestaurantInfo(restaurantId, List.of(productId1, productId2)))
                .willReturn(RestaurantInfo.builder()
                        .restaurantId(restaurantId)
                        .active(true)
                        .products(List.of(ProductInfo.builder()
                                        .productId(productId1)
                                        .name("product1")
                                        .price(new BigDecimal("50.00"))
                                                .available(true)
                                        .build(),
                                ProductInfo.builder()
                                        .productId(productId2)
                                        .name("product2")
                                        .price(new BigDecimal("25.00"))
                                        .available(true)
                                        .build()))
                        .build());
    }
}
