package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private final UUID customerId = UUID.randomUUID();

    @DisplayName("customer가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCustomerDoesNotExist() {
        //given
        given(customerRepository.findById(customerId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderService.checkCustomer(customerId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문자를 찾을 수 없습니다.");
    }

    @DisplayName("customer가 존재하면 예외가 발생하지 않는다.")
    @Test
    void shouldNotThrowException_whenCustomerExists() {
        //given
        Customer customer = Customer.builder()
                .id(customerId)
                .name("유저")
                .build();

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));

        //when, then
        assertDoesNotThrow(() -> orderService.checkCustomer(customerId));
    }

}
