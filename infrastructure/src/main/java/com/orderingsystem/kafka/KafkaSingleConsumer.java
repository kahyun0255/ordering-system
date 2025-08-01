package com.orderingsystem.kafka;

public interface KafkaSingleConsumer<T> {
    void receive(T message, String keys, Integer partitions, Long offsets);
}
