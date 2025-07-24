package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalResponseMessage {

    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID restaurantId;
    private Instant createdAt;
    private OrderApprovalStatus orderApprovalStatus;
    private List<String> failureMessages;

}
