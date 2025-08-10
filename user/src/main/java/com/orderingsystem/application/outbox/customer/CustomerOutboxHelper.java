package com.orderingsystem.application.outbox.customer;

import static com.orderingsystem.common.saga.SagaConstants.CUSTOMER_CREATED_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.application.outbox.customer.model.CustomerEventPayload;
import com.orderingsystem.domain.exception.UserDomainException;
import com.orderingsystem.domain.model.outbox.CustomerOutbox;
import com.orderingsystem.domain.repository.outbox.CustomerOutboxRepository;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerOutboxHelper {

    private final CustomerOutboxRepository customerOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(CustomerOutbox customerOutbox) {
        if (!customerOutboxRepository.existsByTypeAndOutboxStatusAndEventId(CUSTOMER_CREATED_NAME, OutboxStatus.STARTED,
                customerOutbox.getEventId())) {
            customerOutboxRepository.save(customerOutbox);
            log.info("User CustomerOutbox 저장. EventId : {}, Type : {}", customerOutbox.getEventId(),
                    customerOutbox.getType());
        } else {
            log.warn("이미 저장된 User CustomerOutbox가 존재합니다. EventId : {}, Type : {}", customerOutbox.getEventId(),
                    customerOutbox.getType());
        }
    }

    @Transactional
    public void saveCustomerOutboxMessage(CustomerEventPayload customerEventPayload, OutboxStatus outboxStatus, UUID eventId) {
        save(CustomerOutbox.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .createdAt(customerEventPayload.getCreatedAt())
                .type(CUSTOMER_CREATED_NAME)
                .payload(createPayload(customerEventPayload))
                .outboxStatus(outboxStatus)
                .build());
    }

    private String createPayload(CustomerEventPayload customerEventPayload) {
        try {
            return objectMapper.writeValueAsString(customerEventPayload);
        } catch (JsonProcessingException e) {
            log.error("CustomerEventPayload 생성에 실패했습니다. User Id : {}", customerEventPayload.getId());
            throw new UserDomainException(
                    "CustomerEventPayload 생성에 실패했습니다. User Id : " + customerEventPayload.getId());
        }
    }

}
