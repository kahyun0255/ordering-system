package com.orderingsystem.order.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class OrderItemApplicationRequest {

    private final UUID productId;
    private final Integer quantity;
    private final BigDecimal price;
    private final BigDecimal subTotal;

}
