package com.orderingsystem.application.mapper;

import com.orderingsystem.application.outbox.customer.model.CustomerEventPayload;
import com.orderingsystem.domain.event.UserCreatedEvent;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public CustomerEventPayload userCreatedToUserCreateEventPayload(UserCreatedEvent userCreatedEvent) {
        return CustomerEventPayload.builder()
                .username(userCreatedEvent.getUser().getUsername())
                .id(userCreatedEvent.getUser().getUserId().toString())
                .createdAt(ZonedDateTime.now())
                .build();
    }

}
