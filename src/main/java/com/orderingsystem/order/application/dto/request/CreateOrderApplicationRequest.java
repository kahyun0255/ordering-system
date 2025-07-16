package com.orderingsystem.order.application.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class CreateOrderApplicationRequest {

    private final UUID customerId;
    private final UUID restaurantId;
    private final BigDecimal price;
    private final List<OrderItemApplicationRequest> items;
    private final OrderAddressApplicationRequest address;

}
