package com.orderingsystem.order.application.dto.response;

import com.orderingsystem.common.domain.status.OrderStatus;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderStatusResponse {

    private final UUID orderTrackingId;
    private final OrderStatus orderStatus;
    private final List<String> failureMessages;

}
