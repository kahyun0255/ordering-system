package com.orderingsystem.order.presentation.request;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class CreateOrderRequest {

    @NotNull
    private final UUID customerId;

    @NotNull
    private final UUID restaurantId;

    @NotNull
    private final BigDecimal price;

    @NotNull
    private final List<OrderItemRequest> items;

    @NotNull
    private final OrderAddressRequest address;

    public CreateOrderApplicationRequest toApplicationRequest(){
        return CreateOrderApplicationRequest.builder()
                .customerId(this.customerId)
                .restaurantId(this.restaurantId)
                .price(this.price)
                .items(this.items.stream()
                        .map(OrderItemRequest::toApplicationRequest)
                        .collect(Collectors.toList()))
                .address(this.address.toApplicationRequest())
                .build();
    }
}
