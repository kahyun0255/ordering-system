package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.OrderPaymentService;
import com.orderingsystem.order.application.OrderRestaurantApprovalService;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.infra.kafka.message.PaymentResponseDebeziumMessage;
import com.orderingsystem.order.infra.kafka.message.PaymentResponseMessage;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
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
public class PaymentResponseKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OrderPaymentService orderPaymentService;
    private final OrderRestaurantApprovalService orderRestaurantApprovalService;

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
                PaymentResponseDebeziumMessage debeziumMessage =
                        objectMapper.readValue(message, PaymentResponseDebeziumMessage.class);

                if (debeziumMessage.getBefore() == null &&
                        debeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {

                    PaymentResponseMessage paymentResponseMessage = objectMapper.readValue(
                            debeziumMessage.getAfter().getPayload(), PaymentResponseMessage.class);

                    log.info("결제 응답 수신. Order Id : {}, PaymentStatus: {}", paymentResponseMessage.getOrderId(),
                            paymentResponseMessage.getPaymentStatus());

                    if (PaymentStatus.COMPLETED.name().equals(paymentResponseMessage.getPaymentStatus())) {
                        log.info("결제 완료. order Id : {}", paymentResponseMessage.getOrderId());
                        orderPaymentService.process(paymentResponseMessage.toPaymentResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    } else if (PaymentStatus.CANCELLED.name().equals(paymentResponseMessage.getPaymentStatus())) {
                        log.info("결제 취소. order Id : {}", paymentResponseMessage.getOrderId());
                        orderPaymentService.rollback(paymentResponseMessage.toPaymentResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    } else if (PaymentStatus.FAILED.name().equals(paymentResponseMessage.getPaymentStatus())) {
                        log.info("결제 실패. order Id : {}", paymentResponseMessage.getOrderId());
                        orderPaymentService.rollback(paymentResponseMessage.toPaymentResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    } else if (PaymentStatus.REFUNDED.name().equals(paymentResponseMessage.getPaymentStatus())) {
                        orderRestaurantApprovalService.reject(paymentResponseMessage.toPaymentResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("PaymentRequestMessage Json 파싱에 실패했습니다. error: {}", e.toString());
            } catch (OptimisticLockingFailureException e) {
                //NO-OP
                log.error("Caught optimistic locking exception in PaymentResponseKafkaListener");
            } catch (DataAccessException e) {
                Throwable root = e.getRootCause();
                if (root instanceof SQLException sqlEx && "23000".equals(sqlEx.getSQLState())
                        && sqlEx.getErrorCode() == 1062) {
                    //NO-OP
                    log.warn("유니크 제약 위반 발생. Sql Status : {}", sqlEx.getSQLState());
                } else {
                    throw new OrderApplicationException("DB 예외 발생", e);
                }
            }
        });
    }
}
