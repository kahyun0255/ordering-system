package com.orderingsystem.common.domain.event;

public interface DomainEvent<T> {
    void fire();
}
