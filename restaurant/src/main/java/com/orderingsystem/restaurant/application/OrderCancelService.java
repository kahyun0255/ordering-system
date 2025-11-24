package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.ProductRequest;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import com.orderingsystem.restaurant.domain.repository.outbox.ProcessedMessageRepository;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancelService {

    private final ProcessedMessageRepository processedMessageRepository;

    @Transactional
    public boolean checkAndMarkProcessed(ProductRequest productRequest, MessageType messageType){
        int inserted = processedMessageRepository.insertIgnore(
                productRequest.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );
        if (inserted == 0) {
            log.info("중복 메시지 무시. outboxId={}, orderId={}, sagaId={}",
                   productRequest.getId(), productRequest.getOrderId(), productRequest.getSagaId());
            return false;
        }
        return true;
    }

}
