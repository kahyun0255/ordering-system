package com.orderingsystem.restaurant.domain.model.outbox;

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
@Table(name = "restaurant_update_outbox")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantUpdateOutbox {

    @Id
    private UUID id;

    private UUID eventId;
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;

    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Version
    private Long version;

}
