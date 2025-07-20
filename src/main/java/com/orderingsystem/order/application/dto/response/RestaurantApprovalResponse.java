package com.orderingsystem.order.application.dto.response;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantApprovalResponse {

    private UUID id;
    private UUID orderId;
    private UUID restaurantId;
    private Instant createdAt;
    private OrderApprovalStatus orderApprovalStatus;
    private List<String> failureMessages;

}
