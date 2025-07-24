package com.orderingsystem.order.application.publisher;

import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.function.BiConsumer;

public interface RestaurantApprovalRequestMessagePublisher {

    void publish(RestaurantApprovalOutbox restaurantApprovalOutbox,
                   BiConsumer<RestaurantApprovalOutbox, OutboxStatus> outboxCallBack);


}
