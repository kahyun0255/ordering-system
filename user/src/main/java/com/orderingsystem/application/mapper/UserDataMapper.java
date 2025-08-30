package com.orderingsystem.application.mapper;

import com.orderingsystem.application.outbox.model.UserCreatedEventPayload;
import com.orderingsystem.application.outbox.model.UserDeletedEventPayload;
import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.event.UserDeletedEvent;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public UserCreatedEventPayload userCreatedEventToUserCreateEventPayload(UserCreatedEvent userCreatedEvent) {
        return UserCreatedEventPayload.builder()
                .username(userCreatedEvent.getUser().getUsername())
                .id(userCreatedEvent.getUser().getUserId().toString())
                .createdAt(ZonedDateTime.now())
                .type(OutboxEventOperation.INSERT.name())
                .build();
    }

    public UserDeletedEventPayload userDeletedEventToUserDeleteEventPayload(UserDeletedEvent userDeletedEvent) {
        return UserDeletedEventPayload.builder()
                .username(userDeletedEvent.getUser().getUsername())
                .id(userDeletedEvent.getUser().getUserId().toString())
                .type(OutboxEventOperation.DELETE.name())
                .build();
    }

}
