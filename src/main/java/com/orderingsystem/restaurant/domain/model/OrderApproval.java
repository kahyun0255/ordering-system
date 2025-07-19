package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="restaurant_order_approval")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class OrderApproval {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    private UUID restaurantId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private OrderApprovalStatus status;
}
