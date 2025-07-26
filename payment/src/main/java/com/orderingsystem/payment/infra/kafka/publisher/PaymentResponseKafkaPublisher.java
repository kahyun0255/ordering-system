package com.orderingsystem.payment.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.application.outbox.model.OrderEventPayload;
import com.orderingsystem.payment.application.publisher.PaymentResponseMessagePublisher;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import com.orderingsystem.payment.infra.kafka.PaymentMessageConfigData;
import com.orderingsystem.payment.infra.kafka.PaymentMessageDataMapper;
import com.orderingsystem.payment.infra.kafka.message.PaymentResponseMessage;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResponseKafkaPublisher implements PaymentResponseMessagePublisher {

    private final KafkaMessageHelper kafkaMessageHelper;
    private final PaymentMessageDataMapper paymentMessageDataMapper;
    private final ObjectMapper objectMapper;
    private final KafkaProducer<String, String> kafkaProducer;
    private final PaymentMessageConfigData paymentMessageConfigData;

    @Override
    public void publish(OrderOutbox orderOutbox, BiConsumer<OrderOutbox, OutboxStatus> outboxCallback) {
        OrderEventPayload orderEventPayload =
                paymentMessageDataMapper.getOrderPaymentEventPayload(orderOutbox.getPayload(), OrderEventPayload.class);
        UUID sagaId = orderOutbox.getSagaId();

        log.info("Payment OrderOutbox Message를 수신했습니다. Order Id : {}, Saga Id : {}",
                orderEventPayload.getOrderId(), sagaId.toString());

        try {
            PaymentResponseMessage paymentResponseMessage =
                    paymentMessageDataMapper.orderEventPayloadToPaymentResponseMessage(sagaId, orderEventPayload);
            String responseMessage = objectMapper.writeValueAsString(paymentResponseMessage);

            kafkaProducer.send(
                    paymentMessageConfigData.getPaymentResponseTopicName(),
                    sagaId.toString(),
                    responseMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            paymentMessageConfigData.getPaymentResponseTopicName(),
                            responseMessage,
                            orderOutbox,
                            outboxCallback,
                            orderEventPayload.getOrderId()
                    ));

            log.info("OrderEventPayload Kafka 전송. Order Id : {}, Saga Id : {}", orderEventPayload.getOrderId(), sagaId);

        } catch (JsonProcessingException e) {
            log.error("{} 객체 매핑에 실패했습니다. 원인: {}", orderOutbox.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("OrderPaymentEventPayload Kafka 전송에 실패했습니다. Order Id : {} Saga Id : {}, Error : {}",
                    orderEventPayload.getOrderId(), sagaId, e.getMessage());
        }
    }
}
