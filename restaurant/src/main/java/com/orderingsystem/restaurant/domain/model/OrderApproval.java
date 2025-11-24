package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.BaseEntity;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.event.order.orderapproval.OrderApprovedEvent;
import com.orderingsystem.restaurant.domain.event.order.orderapproval.OrderCancelledEvent;
import com.orderingsystem.restaurant.domain.event.order.orderapproval.OrderRejectedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "order_approval")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Slf4j
public class OrderApproval extends BaseEntity {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    private UUID restaurantId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private OrderApprovalStatus status;

    public OrderApprovedEvent approval() {
        if (!this.status.equals(OrderApprovalStatus.ACCEPTED)) {
            log.info("{} 레스토랑에서 {} 주문을 승인할 수 없는 상태입니다. 상태 : {}", this.restaurantId, this.orderId, this.status);
            throw new IllegalArgumentException("주문을 승인할 수 없는 상태입니다.");
        }
        this.status = OrderApprovalStatus.APPROVED;

        return new OrderApprovedEvent(this, this.restaurantId, ZonedDateTime.now());
    }

    public OrderRejectedEvent reject() {
        if (!this.status.equals(OrderApprovalStatus.ACCEPTED)) {
            log.info("{} 레스토랑에서 {} 주문을 거절할 수 없는 상태입니다. 상태 : {}", this.restaurantId, this.orderId, this.status);
            throw new IllegalArgumentException("주문을 승인할 수 없는 상태입니다.");
        }
        this.status = OrderApprovalStatus.REJECTED;

        return new OrderRejectedEvent(this, this.restaurantId, ZonedDateTime.now());
    }

    public OrderCancelledEvent cancel() {
        if (this.status.equals(OrderApprovalStatus.APPROVED) || this.status.equals(OrderApprovalStatus.REJECTED)) {
            log.info("{} 레스토랑에서 {} 주문을 취소할 수 없는 상태입니다. 상태 : {}", this.restaurantId, this.orderId, this.status);
            throw new IllegalArgumentException("주문을 취소할 수 없는 상태입니다.");
        }
        this.status = OrderApprovalStatus.CANCELLED;

        return new OrderCancelledEvent(this, this.restaurantId, ZonedDateTime.now());
    }

}
