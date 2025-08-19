package com.orderingsystem.order.infra.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.order.application.CustomerService;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.infra.kafka.message.CustomerMessage;
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
public class CustomerKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final CustomerService customerService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.customer-group-id}",
            topics = "${order-topic.customer-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 customer request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

        messages.forEach(message -> {
            try {
                String payload = message;
                if (payload.startsWith("\"") && payload.endsWith("\"")) {
                    payload = objectMapper.readValue(payload, String.class);
                }
                CustomerMessage customerMessage = objectMapper.readValue(payload, CustomerMessage.class);

                if (customerMessage.getType().equals("INSERT")) {
                    log.info("customer 수신. customer Id : {}", customerMessage.getId());

                    customerService.createCustomer(customerMessage.toCreateCustomerApplicationRequest());
                }
            } catch (JsonProcessingException e) {
                log.error("CustomerMessage Json 파싱에 실패했습니다. error : {}", e.getMessage());
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
