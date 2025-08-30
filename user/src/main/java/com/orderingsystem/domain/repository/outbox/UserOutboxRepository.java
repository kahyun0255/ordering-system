package com.orderingsystem.domain.repository.outbox;

import com.orderingsystem.domain.model.outbox.UserOutbox;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOutboxRepository extends JpaRepository<UserOutbox, UUID> {

    boolean existsByTypeAndEventId(String type, UUID eventId);

    @Modifying
    @Query(value = "DELETE FROM user_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);
}
