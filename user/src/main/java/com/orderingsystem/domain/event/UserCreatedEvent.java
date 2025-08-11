package com.orderingsystem.domain.event;

import com.orderingsystem.domain.model.User;
import java.time.ZonedDateTime;

public class UserCreatedEvent extends UserEvent {

    public UserCreatedEvent(User user, ZonedDateTime createdAt) {
        super(user, createdAt);
    }

}
