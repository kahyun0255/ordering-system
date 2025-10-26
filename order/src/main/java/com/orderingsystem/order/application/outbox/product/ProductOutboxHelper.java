package com.orderingsystem.order.application.outbox.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.outbox.product.model.OrderProductEventPayload;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.ProductOutbox;
import com.orderingsystem.order.domain.repository.outbox.ProductOutboxRepository;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductOutboxHelper {

    private final ProductOutboxRepository productOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(ProductOutbox productOutbox, String type) {
        if (!productOutboxRepository.existsByTypeAndSagaIdAndSagaStatus(type, productOutbox.getSagaId(),
                productOutbox.getSagaStatus())) {
            productOutboxRepository.save(productOutbox);
            log.info("Order ProductOutbox에 저장했습니다. Outbox Id : {}", productOutbox.getId());
        } else {
            log.info("이미 저장된 Order ProductOutbox 메시지가 존재합니다. Saga Id : {}, Type : {}", productOutbox.getSagaId(),
                    productOutbox.getType());
        }
    }

    @Transactional
    public void saveProductOutboxMessage(OrderProductEventPayload orderProductEventPayload, String type,
                                         SagaStatus sagaStatus, UUID sagaId) {
        save(ProductOutbox.builder()
                        .id(UUID.randomUUID())
                        .sagaId(sagaId)
                        .createdAt(orderProductEventPayload.getCreatedAt())
                        .type(type)
                        .payload(createPayload(orderProductEventPayload))
                        .sagaStatus(sagaStatus)
                        .build(),
                type);
    }

    private String createPayload(OrderProductEventPayload orderProductEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderProductEventPayload);
        } catch (JsonProcessingException e) {
            log.error("orderProductEventPayload 생성에 실패했습니다. Order Id : {}", orderProductEventPayload.getOrderId());
            throw new OrderDomainException(
                    "orderProductEventPayload 생성에 실패했습니다. Order Id : " + orderProductEventPayload.getOrderId());
        }
    }

    @Transactional
    public Optional<ProductOutbox> getProductOutboxBySagaIdAndSagaStatus(UUID sagaId, String type,
                                                                         SagaStatus... sagaStatus) {
        return productOutboxRepository.findByTypeAndSagaIdAndSagaStatusIn(type, sagaId,
                Arrays.asList(sagaStatus));
    }

    @Transactional
    public int deleteOlderThan(ZonedDateTime threshold) {
        return productOutboxRepository.deleteOlderThan(threshold);
    }

}
