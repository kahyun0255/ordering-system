package com.orderingsystem.common.domain.publisher;

import com.orderingsystem.common.domain.event.DomainEvent;

public interface DomainEventPublisher<T extends DomainEvent> {

    void publish(T domainEvent);

}
