package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.order.application.dto.request.CustomerApplicationRequest;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.ProcessedMessage;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @DisplayName("Customer 저장에 성공한다.")
    @Test
    void shouldSaveCustomerSuccessfully() {
        //given
        CustomerApplicationRequest request = CustomerApplicationRequest.builder()
                .id(UUID.randomUUID())
                .username("유저 이름")
                .createdAt(Instant.now())
                .build();

        //when
        customerService.createCustomer(request);

        //then
        Optional<Customer> customer = customerRepository.findById(request.getId());
        assertThat(customer).isPresent();
        assertThat(customer.get().getName()).isEqualTo(request.getUsername());
    }

    @DisplayName("ProcessedMessage에 해당 메시지가 처리되었다고 저장되어있을 경우에는 저장에 실패한다.")
    @Test
    void shouldFailToSave_whenMessageAlreadyProcessed() {
        //given
        CustomerApplicationRequest request = CustomerApplicationRequest.builder()
                .id(UUID.randomUUID())
                .username("유저 이름")
                .createdAt(Instant.now())
                .build();

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(request.getId())
                .messageType(MessageType.CUSTOMER_CREATE)
                .processedAt(ZonedDateTime.now())
                .build());

        //when
        customerService.createCustomer(request);

        //then
        Optional<Customer> customer = customerRepository.findById(request.getId());
        assertThat(customer).isNotPresent();
    }

    @DisplayName("Customer 삭제에 성공한다.")
    @Test
    void shouldDeleteCustomerSuccessfully() {
        //given
        CustomerApplicationRequest request = CustomerApplicationRequest.builder()
                .id(UUID.randomUUID())
                .username("유저 이름")
                .createdAt(Instant.now())
                .build();

        customerRepository.save(Customer.builder()
                .id(request.getId())
                .name(request.getUsername())
                .build());

        assertThat(customerRepository.findById(request.getId())).isPresent();

        //when
        customerService.deleteCustomer(request);

        //then
        assertThat(customerRepository.findById(request.getId())).isNotPresent();
    }

    @DisplayName("ProcessedMessage에 해당 메시지가 처리되었다고 저장되어있을 경우에는 삭제에 실패한다.")
    @Test
    void shouldFailToDelete_whenMessageAlreadyProcessed() {
        //given
        CustomerApplicationRequest request = CustomerApplicationRequest.builder()
                .id(UUID.randomUUID())
                .username("유저 이름")
                .createdAt(Instant.now())
                .build();

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(request.getId())
                .messageType(MessageType.CUSTOMER_CREATE)
                .processedAt(ZonedDateTime.now())
                .build());

        customerRepository.save(Customer.builder()
                .id(request.getId())
                .name(request.getUsername())
                .build());

        assertThat(customerRepository.findById(request.getId())).isPresent();

        //when
        customerService.createCustomer(request);

        //then
        assertThat(customerRepository.findById(request.getId())).isPresent();
    }

}