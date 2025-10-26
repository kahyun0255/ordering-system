package com.orderingsystem.restaurant.infra.kafka.message;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderItemMessage {

    private UUID id;
    private int quantity;

}
