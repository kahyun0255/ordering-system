package com.orderingsystem.kafka;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer<K, V> {

    private final KafkaTemplate<K, V> kafkaTemplate;

    public void send(String topicName, K key, V message, BiConsumer<SendResult<K, V>, Throwable> callback) {
        log.info("Kafka 메시지 전송 message = {} to topic = {}", message, topicName);
        try {
            CompletableFuture<SendResult<K, V>> kafkaResultFuture = kafkaTemplate.send(topicName, key, message);
            kafkaResultFuture.whenComplete(callback);
        } catch (KafkaException e) {
            log.error("Kafka Producer Exception. key : {}, message : {} and exception : {}", key, message,
                    e.getMessage());
            throw new KafkaException("Kafka Producer Exception key : " + key + ", message : " + message);
        }
    }

    @PreDestroy
    public void close() {
        if (kafkaTemplate != null) {
            log.info("Kafka Producer 종료");
            kafkaTemplate.destroy();
        }
    }
}
