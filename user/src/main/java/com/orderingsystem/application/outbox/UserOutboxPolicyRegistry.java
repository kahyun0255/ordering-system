package com.orderingsystem.application.outbox;

import com.orderingsystem.domain.model.UserType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserOutboxPolicyRegistry {

    private final Map<UserType, UserOutboxPolicy> map;

    public UserOutboxPolicyRegistry(List<UserOutboxPolicy> policies) {
        map = policies.stream()
                .collect(Collectors.toMap(UserOutboxPolicy::usertype, p -> p));
    }

    public Optional<UserOutboxPolicy> get(UserType userType) {
        return Optional.ofNullable(map.get(userType));
    }

}
