package com.orderingsystem.payment.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.infrastructure.kafka.KafkaMessageHelper;
import com.orderingsystem.infrastructure.kafka.KafkaProducer;
import com.orderingsystem.payment.application.publisher.PaymentCancelledMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentCancelledEvent;
import com.orderingsystem.payment.infra.kafka.PaymentMessageConfigData;
import com.orderingsystem.payment.infra.kafka.PaymentMessageDataMapper;
import com.orderingsystem.payment.infra.kafka.message.PaymentResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCancelledKafkaPublisher implements PaymentCancelledMessagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final ObjectMapper objectMapper;
    private final PaymentMessageConfigData paymentMessageConfigData;
    private final PaymentMessageDataMapper paymentMessageDataMapper;

    @Override
    public void publish(PaymentCancelledEvent domainEvent) {
        String orderId = domainEvent.getPayment().getOrderId().toString();
        log.info("PaymentCancelledEvent 수신. Order Id :{}", orderId);

        try {
            PaymentResponseMessage paymentResponseMessage = paymentMessageDataMapper.paymentEventToPaymentResponseMessage(
                    domainEvent);
            String responseMessage = objectMapper.writeValueAsString(paymentResponseMessage);

            kafkaProducer.send(
                    paymentMessageConfigData.getPaymentResponseTopicName(),
                    orderId,
                    responseMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            paymentMessageConfigData.getPaymentResponseTopicName(),
                            responseMessage,
                            orderId));

            log.info("PaymentResponseMessage를 Kafka로 전송했습니다. Order Id : {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("PaymentResponseMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
        } catch (Exception e) {
            log.error("PaymentResponseMessage 전송에 실패했습니다. order id : {}, error : {}", orderId, e.getMessage());
        }
    }
}
