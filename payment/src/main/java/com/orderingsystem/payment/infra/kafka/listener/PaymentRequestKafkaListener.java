package com.orderingsystem.payment.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.payment.application.PaymentService;
import com.orderingsystem.payment.infra.kafka.message.PaymentRequestMessage;
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
public class PaymentRequestKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.payment-consumer-group-id}", topics = "${payment-topic.payment-request-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 payment request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                PaymentRequestMessage paymentRequestMessage = objectMapper.readValue(message,
                        PaymentRequestMessage.class);
                log.info("결제 요청 수신. Order Id : {}, PaymentStatus: {}", paymentRequestMessage.getOrderId(),
                        paymentRequestMessage.getPaymentOrderStatus());

                if (PaymentOrderStatus.PENDING.equals(paymentRequestMessage.getPaymentOrderStatus())) {
                    log.info("결제 진행. order Id : {}", paymentRequestMessage.getOrderId());
                    paymentService.completePayment(paymentRequestMessage.toPaymentRequest());
                } else if (PaymentOrderStatus.CANCELLED.equals(paymentRequestMessage.getPaymentOrderStatus())){
                    log.info("결제 취소 진행. order Id : {}", paymentRequestMessage.getOrderId());
                    paymentService.cancelPayment(paymentRequestMessage.toPaymentRequest());
                }

            } catch (JsonProcessingException e) {
                log.error("PaymentRequestMessage Json 파싱에 실패했습니다.");
            } catch (Exception e) {
                log.error("결제 요청 메시지 처리 중 오류 발생. message : {}, error : {}", message, e.getMessage());
            }
        });
    }
}
