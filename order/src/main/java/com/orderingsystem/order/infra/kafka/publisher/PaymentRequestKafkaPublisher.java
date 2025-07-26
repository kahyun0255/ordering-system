package com.orderingsystem.order.infra.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaMessageHelper;
import com.orderingsystem.kafka.KafkaProducer;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.application.publisher.PaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.infra.kafka.OrderMessageConfigData;
import com.orderingsystem.order.infra.kafka.OrderMessagingDataMapper;
import com.orderingsystem.order.infra.kafka.message.PaymentRequestMessage;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestKafkaPublisher implements PaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;
    private final OrderMessageConfigData orderMessageConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(PaymentOutbox paymentOutbox, BiConsumer<PaymentOutbox, OutboxStatus> outboxCallBack) {
        OrderPaymentEventPayload orderPaymentEventPayload = orderMessagingDataMapper.getEventPayload(
                paymentOutbox.getPayload(), OrderPaymentEventPayload.class);
        UUID sagaId = paymentOutbox.getSagaId();

        log.info("Order PaymentOutbox Message를 수신했습니다. Order Id : {}, Saga Id : {}",
                orderPaymentEventPayload.getOrderId(), sagaId.toString());

        try {
            PaymentRequestMessage paymentRequestMessage =
                    orderMessagingDataMapper.orderPaymentEventToPaymentRequestMessage(sagaId, orderPaymentEventPayload);
            String requestMessage = objectMapper.writeValueAsString(paymentRequestMessage);

            kafkaProducer.send(
                    orderMessageConfigData.getPaymentRequestTopicName(),
                    sagaId.toString(),
                    requestMessage,
                    kafkaMessageHelper.getKafkaCallback(
                            orderMessageConfigData.getPaymentRequestTopicName(),
                            requestMessage,
                            paymentOutbox,
                            outboxCallBack,
                            orderPaymentEventPayload.getOrderId()));

            log.info("OrderPaymentEventPayload Kafka 전송. Order Id : {}, Saga Id : {}",
                    orderPaymentEventPayload.getOrderId(), sagaId);
        } catch (JsonProcessingException e) {
            log.error("{} 객체 매핑에 실패했습니다. 원인: {}", paymentOutbox.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("OrderPaymentEventPayload Kafka 전송에 실패했습니다. Order Id : {} Saga Id : {}, Error : {}",
                    orderPaymentEventPayload.getOrderId(), sagaId, e.getMessage());
        }
    }
}
