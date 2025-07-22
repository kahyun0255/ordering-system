package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.BaseEntity;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="order_approval")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class OrderApproval extends BaseEntity {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    private UUID restaurantId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private OrderApprovalStatus status;
}
