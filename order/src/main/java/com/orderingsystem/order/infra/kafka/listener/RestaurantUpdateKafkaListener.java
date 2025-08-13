package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.DebeziumOp;
import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.RestaurantUpdateService;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.infra.kafka.message.RestaurantUpdateDebeziumMessage;
import com.orderingsystem.order.infra.kafka.message.RestaurantUpdateMessage;
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
public class RestaurantUpdateKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final RestaurantUpdateService restaurantUpdateService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-group-id}",
            topics = "${order-topic.restaurant-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 restaurant update request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                RestaurantUpdateDebeziumMessage restaurantUpdateDebeziumMessage =
                        objectMapper.readValue(message, RestaurantUpdateDebeziumMessage.class);

                if (restaurantUpdateDebeziumMessage.getBefore() == null &&
                        restaurantUpdateDebeziumMessage.getOp().equals(DebeziumOp.CREATE.getValue())) {
                    RestaurantUpdateMessage restaurantUpdateMessage = objectMapper.readValue(
                            restaurantUpdateDebeziumMessage.getAfter().getPayload(), RestaurantUpdateMessage.class);

                    if (restaurantUpdateMessage.getType().equals(OutboxEventOperation.INSERT.name())) {
                        log.info("주문 도메인 레스토랑 생성. Restaurant Id : {}", restaurantUpdateMessage.getRestaurantId());
                        restaurantUpdateService.create(restaurantUpdateMessage.toRestaurantUpdateApplicationRequest());
                    } else if (restaurantUpdateMessage.getType().equals(OutboxEventOperation.UPDATE.name())) {
                        log.info("주문 도메인 레스토랑 업데이트. Restaurant Id : {}", restaurantUpdateMessage.getRestaurantId());
                        restaurantUpdateService.update(restaurantUpdateMessage.toRestaurantUpdateApplicationRequest());
                    }

                }
            } catch (JsonProcessingException e) {
                log.error("RestaurantUpdateMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
            } catch (OptimisticLockingFailureException e) {
                //NO-OP
                log.error("Caught optimistic locking exception in CustomerKafkaListener");
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
