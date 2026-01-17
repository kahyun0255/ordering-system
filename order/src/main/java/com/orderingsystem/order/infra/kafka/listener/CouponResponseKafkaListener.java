package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.OrderCouponService;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.infra.kafka.message.CouponResponseDebeziumMessage;
import com.orderingsystem.order.infra.kafka.message.CouponResponseMessage;
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
public class CouponResponseKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OrderCouponService orderCouponService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.coupon-response-consumer-group-id}",
            topics = "${order-topic.coupon-response-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 coupon request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                CouponResponseDebeziumMessage debeziumMessage =
                        objectMapper.readValue(message, CouponResponseDebeziumMessage.class);

                if (debeziumMessage.getBefore() == null &&
                        debeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {

                    CouponResponseMessage responseMessage = objectMapper.readValue(
                            debeziumMessage.getAfter().getPayload(), CouponResponseMessage.class);

                    log.info("쿠폰 응답 수신. Order Id : {}, updatedCount : {}",
                            responseMessage.getOrderId(), responseMessage.getUpdatedCount());

                    if ((responseMessage.getFailureMessages() == null || responseMessage.getFailureMessages().isEmpty())
                            && responseMessage.getUpdatedCount() > 0) {
                        log.info("쿠폰 사용 성공. Order Id : [{}]", responseMessage.getOrderId());
                        orderCouponService.process(responseMessage.toCouponResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    } else {
                        log.info("쿠폰 사용 실패. Order Id : {}, failureMessage: [{}], updatedCount : {}",
                                responseMessage.getOrderId(), responseMessage.getFailureMessages(),
                                responseMessage.getUpdatedCount());
                        orderCouponService.rollback(responseMessage.toCouponResponse(
                                UUID.fromString(debeziumMessage.getAfter().getId())));
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("CouponResponseMessage Json 파싱에 실패했습니다. error : {}", e.toString());
            } catch (OptimisticLockingFailureException e) {
                //NO-OP
                log.error("Caught optimistic locking exception in CouponResponseKafkaListener");
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
