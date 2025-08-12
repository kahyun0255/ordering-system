package com.orderingsystem.application.mapper;

import com.orderingsystem.application.outbox.UserCreatedEventPayload;
import com.orderingsystem.application.outbox.UserOutboxEventOperation;
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
                .type(UserOutboxEventOperation.INSERT.name())
                .build();
    }

}
