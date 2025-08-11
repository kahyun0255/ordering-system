package com.orderingsystem.application.mapper;

import com.orderingsystem.application.outbox.customer.model.CustomerEventPayload;
import com.orderingsystem.domain.model.User;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public CustomerEventPayload userCreatedToUserCreateEventPayload(User user) {
        return CustomerEventPayload.builder()
                .username(user.getUsername())
                .id(user.getUserId().toString())
                .createdAt(ZonedDateTime.now())
                .build();
    }

}
