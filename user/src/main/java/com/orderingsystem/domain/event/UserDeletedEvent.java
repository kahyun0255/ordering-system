package com.orderingsystem.domain.event;

import com.orderingsystem.domain.model.User;
import java.time.ZonedDateTime;

public class UserDeletedEvent extends UserEvent {

    public UserDeletedEvent(User user, ZonedDateTime createdAt) {
        super(user, createdAt);
    }

}
