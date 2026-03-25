package com.orderingsystem.coupon.domain.model.outbox;

import com.orderingsystem.common.saga.SagaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderOutbox {

    @Id
    private UUID id;

    private UUID sagaId;
    private UUID orderId;

    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private String type;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;

    @Version
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderOutbox that = (OrderOutbox) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public void updateProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public void updateSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }

}
