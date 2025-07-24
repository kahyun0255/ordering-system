package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.OrderPaymentService;
import com.orderingsystem.order.infra.kafka.message.PaymentResponseMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentResponseKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OrderPaymentService orderPaymentService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.payment-response-consumer-group-id}",
            topics = "${order-topic.payment-response-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 payment request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                PaymentResponseMessage paymentResponseMessage =
                        objectMapper.readValue(message, PaymentResponseMessage.class);
                log.info("결제 응답 수신. Order Id : {}, PaymentStatus: {}", paymentResponseMessage.getOrderId(),
                        paymentResponseMessage.getPaymentStatus());

                if (PaymentStatus.COMPLETED.name().equals(paymentResponseMessage.getPaymentStatus())) {
                    log.info("결제 완료. order Id : {}", paymentResponseMessage.getOrderId());
                    orderPaymentService.process(paymentResponseMessage.toPaymentResponse());
                } else if (PaymentStatus.CANCELLED.name().equals(paymentResponseMessage.getPaymentStatus())){
                    log.info("결제 취소. order Id : {}", paymentResponseMessage.getOrderId());
                    orderPaymentService.rollback(paymentResponseMessage.toPaymentResponse());
                } else if(PaymentStatus.FAILED.name().equals(paymentResponseMessage.getPaymentStatus())){
                    log.info("결제 실패. order Id : {}", paymentResponseMessage.getOrderId());
                    orderPaymentService.rollback(paymentResponseMessage.toPaymentResponse());
                }
            } catch (JsonProcessingException e) {
                log.error("PaymentRequestMessage Json 파싱에 실패했습니다.");
            } catch (Exception e) {
                log.error("결제 요청 메시지 처리 중 오류 발생. message : {}, error : {}", message, e.getMessage());
            }
        });
    }
}
