package com.orderingsystem.domain.event;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.domain.model.User;
import java.time.ZonedDateTime;
import lombok.Getter;

@Getter
public abstract class UserEvent implements DomainEvent<User> {

    private final User user;
    private final ZonedDateTime createdAt;

    public UserEvent(User user, ZonedDateTime createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

}
