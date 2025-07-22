package com.orderingsystem.restaurant.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.infrastructure.kafka.KafkaMessageHelper;
import com.orderingsystem.infrastructure.kafka.KafkaProducer;
import com.orderingsystem.restaurant.application.publisher.OrderApprovedMessagePublisher;
import com.orderingsystem.restaurant.domain.event.OrderApprovedEvent;
import com.orderingsystem.restaurant.infra.kafka.RestaurantMessageConfigData;
import com.orderingsystem.restaurant.infra.kafka.RestaurantMessagingDataMapper;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantApprovedKafkaPublisher implements OrderApprovedMessagePublisher {

    private final KafkaMessageHelper kafkaMessageHelper;
    private final KafkaProducer<String, String> kafkaProducer;
    private final RestaurantMessageConfigData restaurantMessageConfigData;
    private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OrderApprovedEvent domainEvent) {
        String orderId = domainEvent.getOrderApproval().getOrderId().toString();
        log.info("OrderApprovedEvent 수신. Order Id : {}", orderId);

        try {
            RestaurantApprovalResponseMessage restaurantApprovalResponseMessage =
                    restaurantMessagingDataMapper.orderApprovalEventToRestaurantApprovalResponseMessage(domainEvent);
            String responseMessage = objectMapper.writeValueAsString(restaurantApprovalResponseMessage);

            kafkaProducer.send(restaurantMessageConfigData.getRestaurantApprovalResponseTopicName(),
                    orderId,
                    responseMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            restaurantMessageConfigData.getRestaurantApprovalResponseTopicName(),
                            responseMessage,
                            orderId));

            log.info("RestaurantApprovalResponseMessage를 Kafka로 전송했습니다. Order Id : {}",
                    restaurantApprovalResponseMessage.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("RestaurantApprovalResponseMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
        } catch (Exception e) {
            log.error("RestaurantApprovalResponseMessage 전송에 실패했습니다. order id : {}, error : {}", orderId,
                    e.getMessage());
        }
    }
}
