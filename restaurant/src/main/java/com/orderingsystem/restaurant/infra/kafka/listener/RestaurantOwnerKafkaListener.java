package com.orderingsystem.restaurant.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.restaurant.application.OwnerService;
import com.orderingsystem.restaurant.application.exception.RestaurantApplicationException;
import com.orderingsystem.restaurant.infra.kafka.message.RestaurantOwnerMessage;
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

@RequiredArgsConstructor
@Component
@Slf4j
public class RestaurantOwnerKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OwnerService ownerService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-consumer-group-id}",
            topics = "${restaurant-topic.restaurant-owner-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{}개의 Restaurant Owner 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                String payload = message;
                if (payload.startsWith("\"") && payload.endsWith("\"")) {
                    payload = objectMapper.readValue(payload, String.class);
                }
                RestaurantOwnerMessage restaurantOwnerMessage =
                        objectMapper.readValue(payload, RestaurantOwnerMessage.class);

                if (restaurantOwnerMessage.getType().equals(OutboxEventOperation.INSERT.name())) {
                    log.info("레스토랑 오너 생성 메시지 수신. Owner Id : {}", restaurantOwnerMessage.getId());
                    ownerService.createOwner(restaurantOwnerMessage.toRestaurantOwnerApplicationRequest());
                }else if(restaurantOwnerMessage.getType().equals(OutboxEventOperation.DELETE.name())){
                    log.info("레스토랑 오너 삭제 메시지 수신. Onwer Id : {}", restaurantOwnerMessage.getId());
                    ownerService.deleteOwner(restaurantOwnerMessage.toRestaurantOwnerApplicationRequest());
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
