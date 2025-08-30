package com.orderingsystem.order.domain.repository.outbox;

import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.ProcessedMessage;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {

    @Modifying
    @Query(
            value = "INSERT IGNORE INTO processed_messages (message_id, message_type, processed_at) VALUES (:messageId, :messageType, :processedAt)",
            nativeQuery = true
    )
    int insertIgnore(UUID messageId, String messageType, ZonedDateTime processedAt);

    boolean existsByMessageIdAndMessageType(UUID messageId, MessageType messageType);
}
