package com.orderingsystem.order.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.infrastructure.kafka.KafkaMessageHelper;
import com.orderingsystem.infrastructure.kafka.KafkaProducer;
import com.orderingsystem.order.application.publisher.OrderPaidRestaurantRequestMassagePublisher;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.infra.kafka.OrderMessageConfigData;
import com.orderingsystem.order.infra.kafka.OrderMessagingDataMapper;
import com.orderingsystem.order.infra.kafka.message.RestaurantApprovalRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayOrderKafkaPublisher implements OrderPaidRestaurantRequestMassagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderMessageConfigData orderMessageConfigData;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OrderPaidEvent domainEvent) {
        String orderId = domainEvent.getOrder().getId().toString();
        log.info("OrderCreatedEvent 수신. Order Id :{}", orderId);

        try {
            RestaurantApprovalRequestMessage restaurantApprovalRequestMessage =
                    orderMessagingDataMapper.orderPaidEventToRestaurantApprovalRequestMessage(domainEvent);
            String requestMessage = objectMapper.writeValueAsString(restaurantApprovalRequestMessage);

            kafkaProducer.send(orderMessageConfigData.getRestaurantApprovalRequestTopicName(),
                    orderId,
                    requestMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            orderMessageConfigData.getRestaurantApprovalRequestTopicName(),
                            requestMessage,
                            orderId
                    ));

            log.info("restaurantApprovalRequestMessage를 Kafka로 전송했습니다. order id : {}",
                    restaurantApprovalRequestMessage.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("restaurantApprovalRequestMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
        } catch (Exception e) {
            log.error("restaurantApprovalRequestMessage 전송에 실패했습니다. order id : {}, error : {}", orderId,
                    e.getMessage());
        }
    }
}
