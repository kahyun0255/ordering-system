package com.orderingsystem.restaurant.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.restaurant.application.OrderApprovalService;
import com.orderingsystem.restaurant.application.exception.RestaurantApplicationException;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalRequestDebeziumMessage;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantApprovalRequestMessage;
import java.sql.SQLException;
import java.util.List;
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
public class RestaurantApprovalRequestKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OrderApprovalService orderApprovalService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-consumer-user-group-id}",
            topics = "${restaurant-topic.restaurant-approval-request-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{}개의 Restaurant Approval Request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                RestaurantApprovalRequestDebeziumMessage restaurantApprovalRequestDebeziumMessage =
                        objectMapper.readValue(message, RestaurantApprovalRequestDebeziumMessage.class);

                if (restaurantApprovalRequestDebeziumMessage.getBefore() == null &&
                restaurantApprovalRequestDebeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {

                    RestaurantApprovalRequestMessage requestMessage =
                            objectMapper.readValue(restaurantApprovalRequestDebeziumMessage.getAfter().getPayload(),
                                    RestaurantApprovalRequestMessage.class);

                    log.info("주문 승인 시작. Order Id : {}", requestMessage.getOrderId());
                    orderApprovalService.approveOrder(requestMessage.toApprovalRequest());
                }
            } catch (JsonMappingException e) {
                log.info("Json 매핑에 실패했습니다. error : {}", e.getMessage());
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                log.info("Json 프로세싱에 실패했습니다. error : {}", e.getMessage());
                throw new RuntimeException(e);
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
                    throw new RestaurantApplicationException("DB 예외 발생", e);
                }
            }
        });
    }
}
