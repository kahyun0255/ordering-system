package com.orderingsystem.order.infra.kafka.message;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalOrderItem {

    private UUID productId;
    private int quantity;

}
