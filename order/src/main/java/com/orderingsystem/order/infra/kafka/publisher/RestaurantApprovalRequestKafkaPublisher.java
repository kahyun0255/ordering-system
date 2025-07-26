package com.orderingsystem.order.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventPayload;
import com.orderingsystem.order.application.publisher.RestaurantApprovalRequestMessagePublisher;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.infra.kafka.OrderMessageConfigData;
import com.orderingsystem.order.infra.kafka.OrderMessagingDataMapper;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalRequestMessage;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RestaurantApprovalRequestKafkaPublisher implements RestaurantApprovalRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<String, String> kafkaProducer;
    private final OrderMessageConfigData orderMessageConfigData;
    private final ObjectMapper objectMapper;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(RestaurantApprovalOutbox restaurantApprovalOutbox,
                        BiConsumer<RestaurantApprovalOutbox, OutboxStatus> outboxCallBack) {
        RestaurantApprovalEventPayload restaurantApprovalEventPayload = orderMessagingDataMapper.getEventPayload(
                restaurantApprovalOutbox.getPayload(), RestaurantApprovalEventPayload.class);
        UUID sagaId = restaurantApprovalOutbox.getSagaId();

        log.info("Order RestaurantApprovalOutbox Message를 수신했습니다. Order Id : {}, Saga Id : {}",
                restaurantApprovalEventPayload.getOrderId(), sagaId.toString());

        try {
            RestaurantApprovalRequestMessage restaurantApprovalRequestMessage =
                    orderMessagingDataMapper.restaurantApprovalEventToRestaurantApprovalRequestMessage(
                            sagaId, restaurantApprovalEventPayload);

            String requestMessage = objectMapper.writeValueAsString(restaurantApprovalRequestMessage);

            kafkaProducer.send(
                    orderMessageConfigData.getRestaurantApprovalRequestTopicName(),
                    sagaId.toString(),
                    requestMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            orderMessageConfigData.getRestaurantApprovalRequestTopicName(),
                            requestMessage,
                            restaurantApprovalOutbox,
                            outboxCallBack,
                            restaurantApprovalEventPayload.getOrderId()));

            log.info("RestaurantApprovalEventPayload Kafka 전송. Order Id : {}, Saga Id : {}",
                    restaurantApprovalEventPayload.getOrderId(), sagaId);

        } catch (JsonProcessingException e) {
            log.error("{} 객체 매핑에 실패했습니다. 원인: {}", restaurantApprovalOutbox.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("RestaurantApprovalEventPayload Kafka 전송에 실패했습니다. Order Id : {} Saga Id : {}, Error : {}",
                    restaurantApprovalEventPayload.getOrderId(), sagaId, e.getMessage());
        }
    }
}
