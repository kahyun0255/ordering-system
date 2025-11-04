package com.orderingsystem.restaurant.domain.event.orderaccept;

import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class OrderDeclinedEvent extends OrderAcceptEvent {

    public OrderDeclinedEvent(OrderApproval orderApproval,
                              UUID restaurantId, UUID sagaId, List<String> failureMessages,
                              ZonedDateTime createdAt) {
        super(orderApproval, restaurantId, sagaId, failureMessages, createdAt);
    }

}
