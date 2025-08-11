package com.orderingsystem.application.outbox;

import com.orderingsystem.domain.model.User;
import com.orderingsystem.domain.model.UserType;

public interface UserOutboxPolicy {
    UserType usertype();
    void saveOutbox(User user);
}
