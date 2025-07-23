package com.orderingsystem.outbox;

public interface OutboxScheduler {
    void processOutboxMessage();
}
