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
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
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
class OrderFacadeTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private RestaurantApi restaurantApi;

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
    private final Money product1Price = new Money(new BigDecimal("25.00"));
    private final Money product2Price = new Money(new BigDecimal("20.00"));

    @DisplayName("주문 물품이 1개인 경우, 주문 생성에 성공한다.")
    @Test
    void createOrder_withOneProduct() {
        //given
        CreateOrderApplicationRequest request = getOneProductCreateOrderApplicationRequest();

        //when
        CreateOrderResponse response = orderFacade.createOrder(request);

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
        CreateOrderResponse response = orderFacade.createOrder(request);

        //then
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getMessage()).isEqualTo("주문이 성공적으로 생성되었습니다.");
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
        assertThatThrownBy(() -> orderFacade.createOrder(request))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문자를 찾을 수 없습니다.");
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
