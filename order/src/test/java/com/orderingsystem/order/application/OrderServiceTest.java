package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.restaurant.Product;
import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.model.restaurant.RestaurantProduct;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.restaurant.ProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantProductRepository;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantProductRepository restaurantProductRepository;

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

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .active(true)
                .name("restaurant")
                .build());

        productRepository.save(Product.builder()
                .productId(productId1)
                .price(product1Price)
                .available(true)
                .name("product1")
                .build());

        productRepository.save(Product.builder()
                .productId(productId2)
                .price(product2Price)
                .available(true)
                .name("product2")
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

    }

    private final UUID customerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();
    private final Money product1Price = new Money(new BigDecimal("25.00"));
    private final Money product2Price = new Money(new BigDecimal("20.00"));

    @DisplayName("주문 물품이 1개인 경우, 주문 생성에 성공한다.")
    @Test
    void createOrder_withOneProduct() {
        //given
        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

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
        CreateOrderApplicationRequest request = CreateOrderApplicationRequest.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(BigDecimal.valueOf(100000L))
                .items(List.of(OrderItemApplicationRequest.builder()
                        .productId(productId1)
                        .quantity(1)
                        .price(BigDecimal.valueOf(100000L))
                        .subTotal(BigDecimal.valueOf(100000L))
                        .build()))
                .build();

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
        CreateOrderApplicationRequest request = CreateOrderApplicationRequest.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .address(OrderAddressApplicationRequest.builder()
                        .street("street1")
                        .postalCode("123-78")
                        .city("city1")
                        .build())
                .price(product2Price.multiply(2).getAmount().add(BigDecimal.valueOf(100000L)))
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(BigDecimal.valueOf(100000L))
                                .subTotal(BigDecimal.valueOf(100000L))
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(product2Price.getAmount())
                                .subTotal(product2Price.multiply(2).getAmount())
                                .build()))
                .build();

        //when
        CreateOrderResponse createdOrder = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(createdOrder.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo("상품 : " + productId1 + "의 항목 가격 : "+product1Price.getAmount()+"이 유효하지 않습니다.");
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

        //when, then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문자를 찾을 수 없습니다.");
    }

    @DisplayName("레스토랑이 활성화 상태가 아니라면 주문 생성에 실패한다.")
    @Test
    void failToCreateOrder_whenNotActiveRestaurant() {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("restaurant")
                .active(false)
                .build());

        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

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
                                .price(product1Price.getAmount())
                                .subTotal(product1Price.getAmount())
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(product2Price.getAmount())
                                .subTotal(product2Price.multiply(2).getAmount())
                                .build()))
                .build();

        //when
        CreateOrderResponse createdOrder = orderService.createOrder(request);

        // then
        Optional<Order> order = orderRepository.findByTrackingId(createdOrder.getOrderTrackingId());
        assertThat(order).isPresent();
        assertThat(order.get().getFailureMessages()).isEqualTo(
                "총 주문 금액 : 20000.00 개별 항목들의 합계 : "+product1Price.add(product2Price.multiply(2)).getAmount()+". 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다.");
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
                .price(product1Price.getAmount())
                .items(List.of(OrderItemApplicationRequest.builder()
                        .productId(productId1)
                        .quantity(1)
                        .price(product1Price.getAmount())
                        .subTotal(product1Price.getAmount())
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
                .price(product1Price.add(product2Price.multiply(2)).getAmount())
                .items(List.of(OrderItemApplicationRequest.builder()
                                .productId(productId1)
                                .quantity(1)
                                .price(product1Price.getAmount())
                                .subTotal(product1Price.getAmount())
                                .build(),
                        OrderItemApplicationRequest.builder()
                                .productId(productId2)
                                .quantity(2)
                                .price(product2Price.getAmount())
                                .subTotal(product2Price.multiply(2).getAmount())
                                .build()))
                .build();
    }
}
