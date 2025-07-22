package com.orderingsystem.order.presentation.request;

import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class OrderItemRequest {

    @NotNull
    private final UUID productId;

    @NotNull
    private final Integer quantity;

    @NotNull
    private final BigDecimal price;

    @NotNull
    private final BigDecimal subTotal;

    public OrderItemApplicationRequest toApplicationRequest(){
        return OrderItemApplicationRequest.builder()
                .productId(this.productId)
                .quantity(this.quantity)
                .price(this.price)
                .subTotal(this.subTotal)
                .build();
    }
}
