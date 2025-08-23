package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderAddress;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderAddressRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.OrderValidateAndInitiateService;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceExceptionTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderDataMapper orderDataMapper;

    @Mock
    private OrderValidateAndInitiateService orderValidateAndInitiateService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderAddressRepository orderAddressRepository;

    private final UUID customerId = UUID.randomUUID();
    private final CreateOrderApplicationRequest request = mock(CreateOrderApplicationRequest.class);
    private final RestaurantInfo restaurantInfo = mock(RestaurantInfo.class);
    private final Order order = mock(Order.class);
    private final OrderAddress orderAddress = mock(OrderAddress.class);
    private final OrderCreateEvent orderCreateEvent = mock(OrderCreateEvent.class);

    @DisplayName("Order 저장에 실패할 경우 예외가 발생한다.")
    @Test
    void saveOrder_throwsException_whenRepositoryFails() {
        // given
        given(request.getCustomerId()).willReturn(customerId);
        given(customerRepository.findById(customerId)).willReturn(Optional.of(mock(Customer.class)));
        given(orderDataMapper.orderAddressToStreetAddress(any())).willReturn(orderAddress);
        given(orderDataMapper.createOrderRequestToOrder(any(), any())).willReturn(order);
        given(orderValidateAndInitiateService.validateAndInitiate(any(), any())).willReturn(orderCreateEvent);
        given(orderRepository.save(any())).willReturn(new RuntimeException("DB 에러"));

        // when, then
        assertThatThrownBy(() -> orderService.createOrder(request, new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문이 저장되지 않았습니다.");
    }

    @DisplayName("OrderAddress 저장에 실패할 경우 예외가 발생한다.")
    @Test
    void saveOrderAddress_throwsException_whenRepositoryFails() {
        // given
        given(request.getCustomerId()).willReturn(customerId);
        given(customerRepository.findById(customerId)).willReturn(Optional.of(mock(Customer.class)));
        given(orderDataMapper.orderAddressToStreetAddress(any())).willReturn(orderAddress);
        given(orderDataMapper.createOrderRequestToOrder(any(), any())).willReturn(order);
        given(orderValidateAndInitiateService.validateAndInitiate(any(), any())).willReturn(orderCreateEvent);
        given(orderRepository.save(any())).willReturn(order);
        doThrow(new RuntimeException("주소 저장 실패")).when(orderAddressRepository).save(any());

        // when, then
        assertThatThrownBy(() -> orderService.createOrder(request, new ArrayList<>()))
                .isInstanceOf(OrderDomainException.class)
                .hasMessage("주문 주소가 저장되지 않았습니다.");
    }
}
