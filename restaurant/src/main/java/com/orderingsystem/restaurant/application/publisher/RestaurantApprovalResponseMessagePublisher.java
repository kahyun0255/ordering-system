package com.orderingsystem.restaurant.application.publisher;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.function.BiConsumer;

public interface RestaurantApprovalResponseMessagePublisher {

    void publish(OrderOutbox orderOutbox, BiConsumer<OrderOutbox, OutboxStatus> outboxCallback);

}
