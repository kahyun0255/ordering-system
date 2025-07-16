package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderApproval {

    private final UUID orderApprovalId;
    private final UUID restaurantId;
    private final UUID productId;
    private final UUID orderId;
    private final OrderApprovalStatus approvalStatus;

}
