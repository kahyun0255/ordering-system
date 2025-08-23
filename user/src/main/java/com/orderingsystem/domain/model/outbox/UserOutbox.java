package com.orderingsystem.domain.model.outbox;

import com.orderingsystem.domain.model.UserType;
import com.orderingsystem.outbox.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserOutbox {

    @Id
    private UUID id;

    private UUID eventId;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;

    @Version
    private Long version;

    public void updateProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

}
