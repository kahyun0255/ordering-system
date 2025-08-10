package com.orderingsystem.application.outbox;

import com.orderingsystem.application.mapper.UserDataMapper;
import com.orderingsystem.application.outbox.customer.CustomerOutboxHelper;
import com.orderingsystem.domain.event.UserCreatedEvent;
import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerOutboxPolicy implements UserOutboxPolicy {

    private final CustomerOutboxHelper customerOutboxHelper;
    private final UserDataMapper userDataMapper;

    @Override
    public UserType usertype() {
        return UserType.CUSTOMER;
    }

    @Override
    public void saveOutbox(UserCreatedEvent userCreatedEvent) {
        customerOutboxHelper.saveCustomerOutboxMessage(
                userDataMapper.userCreatedToUserCreateEventPayload(userCreatedEvent),
                OutboxStatus.STARTED,
                UUID.randomUUID());
    }
}
