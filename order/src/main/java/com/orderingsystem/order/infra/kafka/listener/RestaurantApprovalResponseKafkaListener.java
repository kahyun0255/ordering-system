package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.OrderRestaurantApprovalService;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalResponseMessage;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantApprovalResponseKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OrderRestaurantApprovalService orderRestaurantApprovalService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-response-consumer-group-id}",
            topics = "${order-topic.restaurant-approval-response-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 Restaurant Approval Response 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                RestaurantApprovalResponseMessage responseMessage =
                        objectMapper.readValue(message, RestaurantApprovalResponseMessage.class);

                if (OrderApprovalStatus.APPROVED.name().equals(responseMessage.getOrderApprovalStatus().name())) {
                    log.info("주문 승인 진행. Order Id : {}", responseMessage.getOrderId());
                    orderRestaurantApprovalService.process(responseMessage.toRestaurantApprovalResponse());
                } else if (OrderApprovalStatus.REJECTED.name()
                        .equals(responseMessage.getOrderApprovalStatus().name())) {
                    log.info("주문 거절 진행. Order Id :{}", responseMessage.getOrderId());
                    orderRestaurantApprovalService.rollback(responseMessage.toRestaurantApprovalResponse());
                }

            } catch (JsonMappingException e) {
                log.error("RestaurantApprovalResponseMessage Json 매핑에 실패했습니다. error : {}", e.getMessage());
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                log.error("RestaurantApprovalResponseMessage Json 프로세싱에 실패했습니다. error : {}", e.getMessage());
                throw new RuntimeException(e);
            } catch (OptimisticLockingFailureException e) {
                //NO-OP
                log.error("Caught optimistic locking exception in RestaurantApprovalResponseKafkaListener");
            }
        });
    }
}
