package com.orderingsystem.infrastructure.kafka;

import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaMessageHelper {

    public <T> BiConsumer<SendResult<String, T>, Throwable> getKafkaCallback(
            String responseTopicName, T message, String orderId, String messageType) {

        return (sendResult, throwable) -> {
            if (throwable != null) {
                log.error("Kafka 메시지 전송 중 오류가 발생했습니다. topic : {}, message : {}, topic : {}",
                        messageType, message.toString(), responseTopicName, throwable);
            } else {
                RecordMetadata metadata = sendResult.getRecordMetadata();
                log.info("Kafka 메시지를 성공적으로 전송했습니다. order id: {} Topic: {} Partition: {} Offset: {} Timestamp: {}",
                        orderId,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp());
            }
        };
    }
}
