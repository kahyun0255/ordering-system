package com.orderingsystem.restaurant.infra.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderingsystem.kafka.KafkaConsumer;
import com.orderingsystem.restaurant.application.OwnerCreateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class RestaurantOwnerCreateKafkaListener implements KafkaConsumer<String> {

    private final ObjectMapper objectMapper;
    private final OwnerCreateService ownerCreateService;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-consumer-group-id}",
            topics = "${restaurant-topic.restaurant-approval-request-topic-name}")
    public void receive(@Payload List<String> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{}개의 Restaurant Approval Request 메시지를 받았습니다. keys : {}, partitions : {}, offsets : {}",
                messages.size(), keys.toString(), partitions.toString(), offsets.toString());

    }

}
