package com.orderingsystem.common.saga;

import com.orderingsystem.common.domain.event.DomainEvent;

public final class EmptyEvent implements DomainEvent<Void> {

    public static final EmptyEvent INSTANCE = new EmptyEvent();

    private EmptyEvent() {

    }

    @Override
    public void fire() {

    }
}
