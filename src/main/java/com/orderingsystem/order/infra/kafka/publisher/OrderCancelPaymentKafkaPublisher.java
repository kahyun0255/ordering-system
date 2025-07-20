package com.orderingsystem.order.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.infrastructure.kafka.KafkaMessageHelper;
import com.orderingsystem.infrastructure.kafka.KafkaProducer;
import com.orderingsystem.order.application.publisher.OrderCancelledPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.infra.kafka.OrderMessageConfigData;
import com.orderingsystem.order.infra.kafka.OrderMessagingDataMapper;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelPaymentKafkaPublisher implements OrderCancelledPaymentRequestMessagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderMessageConfigData orderMessageConfigData;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(OrderCancelledEvent domainEvent) {
        String orderId = domainEvent.getOrder().getId().toString();
        log.info("OrderCancelledEvent 수신. Order Id :{}", orderId);

        try {
            PaymentRequestMessage paymentRequestMessage =
                    orderMessagingDataMapper.orderCancelledEventToPaymentRequestMessage(domainEvent);
            String requestMessage = objectMapper.writeValueAsString(paymentRequestMessage);

            kafkaProducer.send(
                    orderMessageConfigData.getPaymentRequestTopicName(),
                    orderId,
                    requestMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            orderMessageConfigData.getPaymentRequestTopicName(),
                            requestMessage,
                            orderId));

            log.info("PaymentRequestMessage를 Kafka로 전송했습니다. order id : {}", paymentRequestMessage.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("PaymentRequestMessage Json 프로세싱에 실패했습니다. error : {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
