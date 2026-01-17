package com.orderingsystem.coupon.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.CouponActions;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.coupon.application.CouponRedemptionService;
import com.orderingsystem.coupon.domain.exception.CouponApplicationException;
import com.orderingsystem.coupon.infra.kafka.message.CouponRequestDebeziumMessage;
import com.orderingsystem.coupon.infra.kafka.message.CouponRequestMessage;
import com.orderingsystem.kafka.KafkaSingleConsumer;
import java.sql.SQLException;
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
public class CouponRequestKafkaListener implements KafkaSingleConsumer<String> {

    private final CouponRedemptionService couponRedemptionService;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.coupon-redemption-group-id}",
            topics = "${coupon-service.coupon-request-topic-name}",
            concurrency = "${coupon-service.concurrency}")
    public void receive(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                        @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            CouponRequestDebeziumMessage debeziumMessage = objectMapper.readValue(message,
                    CouponRequestDebeziumMessage.class);

            if (debeziumMessage.getBefore() == null &&
                    debeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {
                log.info("CouponRequestKafkaListener에서 메시지를 받았습니다. message : {}, key : {}, partition : {}, offset: {}",
                        message, key, partition, offset);

                CouponRequestMessage requestMessage = objectMapper.readValue(
                        debeziumMessage.getAfter().getPayload(),
                        CouponRequestMessage.class);

                if (requestMessage.getAction().equals(CouponActions.USE.name())) {
                    log.info("쿠폰 사용 요청 수신. Order Id : [{}], Issued Coupon Id : [{}], User Id : [{}]",
                            requestMessage.getOrderId(), requestMessage.getIssuedCouponId().toString(),
                            requestMessage.getCustomerId());

                    couponRedemptionService.redeem(
                            requestMessage.toCouponRequest(UUID.fromString(debeziumMessage.getAfter().getId())));
                } else if (requestMessage.getAction().equals(CouponActions.ROLLBACK.name())) {
                    log.info("쿠폰 사용 롤백 요청 수신. Order Id : [{}], Issued Coupon Id : [{}], User Id : [{}]",
                            requestMessage.getOrderId(), requestMessage.getIssuedCouponId().toString(),
                            requestMessage.getCustomerId());

                    couponRedemptionService.cancelRedemption(
                            requestMessage.toCouponRequest(UUID.fromString(debeziumMessage.getAfter().getId())));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("CouponRequestMessage Json 파싱에 실패했습니다.", e);
        } catch (OptimisticLockingFailureException e) {
            //NO-OP
            log.error("Caught optimistic locking exception in CouponRequestKafkaListener");
        } catch (DataAccessException e) {
            Throwable root = e.getRootCause();
            if (root instanceof SQLException sqlEx && "23000".equals(sqlEx.getSQLState())
                    && sqlEx.getErrorCode() == 1062) {
                //NO-OP
                log.warn("유니크 제약 위반 발생. Sql Status : {}", sqlEx.getSQLState());
            } else {
                throw new CouponApplicationException("DB 예외 발생", e);
            }
        }
    }

}

