package com.orderingsystem.infrastructure.kafka;

import java.util.List;

public interface KafkaConsumer<T> {
    void receive(List<T> message, List<String> keys, List<Integer> partitions, List<Long> offsets);
}
