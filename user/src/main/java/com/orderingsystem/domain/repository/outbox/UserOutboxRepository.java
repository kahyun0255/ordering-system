package com.orderingsystem.domain.repository.outbox;

import com.orderingsystem.domain.model.outbox.UserOutbox;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOutboxRepository extends JpaRepository<UserOutbox, UUID> {

    boolean existsByTypeAndOutboxStatusAndEventId(String type, OutboxStatus outboxStatus, UUID eventId);
}
