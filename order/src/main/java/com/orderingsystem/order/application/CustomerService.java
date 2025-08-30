package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CustomerApplicationRequest;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    @Transactional
    public void createCustomer(CustomerApplicationRequest customerApplicationRequest) {
        if (markIfNotProcessed(customerApplicationRequest, MessageType.CUSTOMER_CREATE)) {
            return;
        }

        log.info("Customer 생성. Customer Id : {}", customerApplicationRequest.getId());

        customerRepository.save(Customer.builder()
                .id(customerApplicationRequest.getId())
                .name(customerApplicationRequest.getUsername())
                .build());
    }

    @Transactional
    public void deleteCustomer(CustomerApplicationRequest customerApplicationRequest) {
        if (markIfNotProcessed(customerApplicationRequest, MessageType.CUSTOMER_DELETE)) {
            return;
        }

        log.info("Customer 삭제. Customer Id : {}", customerApplicationRequest.getId());

        customerRepository.deleteById(customerApplicationRequest.getId());
    }

    private boolean markIfNotProcessed(CustomerApplicationRequest customerApplicationRequest,
                                       MessageType ownerCreate) {
        int inserted = processedMessageRepository.insertIgnore(
                customerApplicationRequest.getId(),
                ownerCreate.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 처리된 Customer {} 메시지입니다. Customer Id : {}", ownerCreate, customerApplicationRequest.getId());
            return true;
        }
        return false;
    }

}
