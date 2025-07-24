package com.orderingsystem.restaurant.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.outbox.model.OrderEventPayload;
import com.orderingsystem.restaurant.application.publisher.RestaurantApprovalResponseMessagePublisher;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import com.orderingsystem.restaurant.infra.kafka.RestaurantMessageConfigData;
import com.orderingsystem.restaurant.infra.kafka.RestaurantMessagingDataMapper;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalResponseMessage;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantApprovalResponseKafkaPublisher implements RestaurantApprovalResponseMessagePublisher {

    private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;
    private final ObjectMapper objectMapper;
    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final RestaurantMessageConfigData restaurantMessageConfigData;

    @Override
    public void publish(OrderOutbox orderOutbox, BiConsumer<OrderOutbox, OutboxStatus> outboxCallback) {
        OrderEventPayload orderEventPayload = restaurantMessagingDataMapper.getOrderEventPayload(
                orderOutbox.getPayload(), OrderEventPayload.class);
        UUID sagaId = orderOutbox.getSagaId();

        log.info("Restaurant OrderOutbox Message를 수신했습니다. Order Id : {}, Saga Id : {}",
                orderEventPayload.getOrderId(), sagaId.toString());

        try {
            RestaurantApprovalResponseMessage restaurantApprovalResponseMessage =
                    restaurantMessagingDataMapper.orderEventPayloadToRestaurantApprovalResponseMessage
                            (orderEventPayload, sagaId);
            String responseMessage = objectMapper.writeValueAsString(restaurantApprovalResponseMessage);

            kafkaProducer.send(
                    restaurantMessageConfigData.getRestaurantApprovalResponseTopicName(),
                    sagaId.toString(),
                    responseMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            restaurantMessageConfigData.getRestaurantApprovalResponseTopicName(),
                            responseMessage,
                            orderOutbox,
                            outboxCallback,
                            orderEventPayload.getOrderId()));

            log.info("OrderEventPayload Kafka 전송. Order Id : {}, Saga Id : {}", orderEventPayload.getOrderId(), sagaId);

        } catch (JsonProcessingException e) {
            log.error("{} 객체 매핑에 실패했습니다. 원인: {}", orderOutbox.getId(), e.getMessage());
            throw new RuntimeException(orderOutbox.getId() + " 객체 매핑에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("OrderEventPayload Kafka 전송에 실패했습니다. Order Id : {} Saga Id : {}, Error : {}",
                    orderEventPayload.getOrderId(), sagaId, e.getMessage());
            throw new RuntimeException("OrderEventPayload Kafka 전송에 실패했습니다. Order Id : "
                    + orderEventPayload.getOrderId() + " Saga Id : " + sagaId + ", Error : " + e.getMessage());
        }
    }
}
