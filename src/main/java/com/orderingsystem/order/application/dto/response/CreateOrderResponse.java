package com.orderingsystem.order.application.dto.response;

import com.orderingsystem.common.domain.status.OrderStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class CreateOrderResponse {

    @NotNull
    private final UUID orderTrackingId;

    @NotNull
    private final OrderStatus orderStatus;

    @NotNull
    private final String message;

}
