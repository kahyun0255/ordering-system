package com.orderingsystem.domain.repository.outbox;

import com.orderingsystem.domain.model.outbox.UserOutbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOutboxRepository extends JpaRepository<UserOutbox, UUID> {

    boolean existsByTypeAndEventId(String type, UUID eventId);
}
