package com.orderingsystem.payment.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.payment.application.publisher.PaymentFailedMessagePublisher;
import com.orderingsystem.payment.domain.event.PaymentFailedEvent;
import com.orderingsystem.payment.infra.kafka.PaymentMessageConfigData;
import com.orderingsystem.payment.infra.kafka.PaymentMessageDataMapper;
import com.orderingsystem.payment.infra.kafka.message.PaymentResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentFailedKafkaPublisher implements PaymentFailedMessagePublisher {

    private final KafkaProducer<String, String> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final ObjectMapper objectMapper;
    private final PaymentMessageConfigData paymentMessageConfigData;
    private final PaymentMessageDataMapper paymentMessageDataMapper;

    @Override
    public void publish(PaymentFailedEvent domainEvent) {
        String orderId = domainEvent.getPayment().getOrderId().toString();
        log.info("PaymentFailedEvent 수신. Order Id :{}", orderId);

        try {
            PaymentResponseMessage paymentResponseMessage =
                    paymentMessageDataMapper.paymentEventToPaymentResponseMessage(domainEvent);
            String responseMessage = objectMapper.writeValueAsString(paymentResponseMessage);

            kafkaProducer.send(paymentMessageConfigData.getPaymentResponseTopicName(),
                    orderId,
                    responseMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            paymentMessageConfigData.getPaymentResponseTopicName(),
                            responseMessage,
                            orderId
                    ));

            log.info("PaymentResponseMessage를 Kafka로 전송했습니다. Order Id : {}", orderId);
        } catch (JsonProcessingException e) {
            log.error("PaymentRequestMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
        } catch (Exception e){
            log.error("PaymentRequestMessage 전송에 실패했습니다. order id : {}, error : {}", orderId, e.getMessage());
        }
    }
}
