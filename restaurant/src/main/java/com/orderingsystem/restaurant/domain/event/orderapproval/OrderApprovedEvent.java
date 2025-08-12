package com.orderingsystem.restaurant.domain.event.orderapproval;

import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class OrderApprovedEvent extends OrderApprovalEvent {

    public OrderApprovedEvent(OrderApproval orderApproval, UUID restaurantId, List<String> failureMessages,
                              ZonedDateTime createdAt){
        super(orderApproval, restaurantId, failureMessages, createdAt);
    }

}
