package com.orderingsystem.payment.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.kafka.KafkaSingleConsumer;
import com.orderingsystem.payment.application.PaymentService;
import com.orderingsystem.payment.application.exception.PaymentApplicationException;
import com.orderingsystem.payment.infra.kafka.message.PaymentRequestDebeziumMessage;
import com.orderingsystem.payment.infra.kafka.message.PaymentRequestMessage;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentRequestKafkaListener implements KafkaSingleConsumer<String> {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.payment-consumer-group-id}", topics = "${payment-topic.payment-request-topic-name}")
    public void receive(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                        @Header(KafkaHeaders.OFFSET) Long offset) {

        try {
            PaymentRequestDebeziumMessage debeziumMessage = objectMapper.readValue(message, PaymentRequestDebeziumMessage.class);

            if (debeziumMessage.getBefore() == null && debeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {
                log.info("PaymentRequestKafkaListener에서 메시지를 받았습니다. message : {}, key : {}, partition : {}, offset: {}",
                        message, key, partition, offset);

                PaymentRequestDebeziumMessage.Payload payload = debeziumMessage.getAfter();
                PaymentRequestMessage requestMessage = objectMapper.readValue(payload.getPayload(),
                        PaymentRequestMessage.class);

                if (PaymentOrderStatus.PENDING.equals(requestMessage.getPaymentOrderStatus())) {
                    log.info("결제 진행. order Id : {}", requestMessage.getOrderId());
                    paymentService.completePayment(requestMessage.toPaymentRequest());
                } else if (PaymentOrderStatus.CANCELLED.equals(requestMessage.getPaymentOrderStatus())) {
                    log.info("결제 취소 진행. order Id : {}", requestMessage.getOrderId());
                    paymentService.cancelPayment(requestMessage.toPaymentRequest());
                }
            }

        } catch (JsonProcessingException e) {
            log.error("PaymentRequestMessage Json 파싱에 실패했습니다.", e);
        } catch (OptimisticLockingFailureException e) {
            //NO-OP
            log.error("Caught optimistic locking exception in RestaurantApprovalResponseKafkaListener");
        } catch (DataAccessException e) {
            Throwable root = e.getRootCause();
            if (root instanceof SQLException sqlEx && "23000".equals(sqlEx.getSQLState())
                    && sqlEx.getErrorCode() == 1062) {
                //NO-OP
                log.warn("유니크 제약 위반 발생. Sql Status : {}", sqlEx.getSQLState());
            } else {
                throw new PaymentApplicationException("DB 예외 발생", e);
            }
        }
    }
}
