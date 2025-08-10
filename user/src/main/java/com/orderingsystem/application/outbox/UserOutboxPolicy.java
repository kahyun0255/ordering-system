package com.orderingsystem.application.outbox;

import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.UserType;

public interface UserOutboxPolicy {
    UserType usertype();
    void saveOutbox(UserCreatedEvent userCreatedEvent);
}
