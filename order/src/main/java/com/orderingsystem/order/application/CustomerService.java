package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CustomerApplicationRequest;
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
    public void createCustomer(CustomerApplicationRequest customerApplicationRequest) {
        log.info("Customer 생성. Customer Id : {}", customerApplicationRequest.getId());

        customerRepository.save(Customer.builder()
                .id(customerApplicationRequest.getId())
                .name(customerApplicationRequest.getUsername())
                .build());
    }

    @Transactional
    public void deleteCustomer(CustomerApplicationRequest customerApplicationRequest) {
        log.info("Customer 삭제. Customer Id : {}", customerApplicationRequest.getId());

        customerRepository.deleteById(customerApplicationRequest.getId());
    }

}
