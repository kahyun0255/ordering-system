package com.orderingsystem.restaurant.domain.event.orderapproval;

import com.orderingsystem.common.domain.event.DomainEvent;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class OrderApprovalEvent implements DomainEvent<OrderApproval> {

    private final OrderApproval orderApproval;
    private final UUID restaurantId;
    private final ZonedDateTime createdAt;

}
