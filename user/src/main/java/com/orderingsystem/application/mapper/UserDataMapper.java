package com.orderingsystem.application.mapper;

import com.orderingsystem.application.outbox.UserCreatedEventPayload;
import com.orderingsystem.application.outbox.UserDeletedEventPayload;
import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.domain.model.User;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public UserCreatedEventPayload userCreatedToUserCreateEventPayload(User user) {
        return UserCreatedEventPayload.builder()
                .username(user.getUsername())
                .id(user.getUserId().toString())
                .createdAt(ZonedDateTime.now())
                .type(OutboxEventOperation.INSERT.name())
                .build();
    }

    public UserDeletedEventPayload userDeleteToUserDeleteEventPayload(User user) {
        return UserDeletedEventPayload.builder()
                .username(user.getUsername())
                .id(user.getUserId().toString())
                .type(OutboxEventOperation.DELETE.name())
                .build();
    }

}
