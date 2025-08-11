package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateCustomerApplicationRequest;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public void createCustomer(CreateCustomerApplicationRequest createCustomerApplicationRequest) {
        log.info("Customer 생성. Customer Id : {}", createCustomerApplicationRequest.getId());
        customerRepository.save(Customer.builder()
                .id(createCustomerApplicationRequest.getId())
                .name(createCustomerApplicationRequest.getUsername())
                .build());
    }

}
