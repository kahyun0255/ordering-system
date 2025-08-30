package com.orderingsystem.order.domain.model.outbox;

public enum MessageType {
    PAYMENT_COMPLETE, PAYMENT_ROLLBACK, RESTAURANT_APPROVAL, RESTAURANT_REJECT
}
