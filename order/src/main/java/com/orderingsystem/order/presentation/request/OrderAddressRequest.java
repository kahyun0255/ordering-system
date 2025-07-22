package com.orderingsystem.order.presentation.request;

import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class OrderAddressRequest {

    @NotNull
    @Max(value = 50)
    private final String street;

    @NotNull
    @Max(value = 10)
    private final String postalCode;

    @NotNull
    @Max(value = 50)
    private final String city;

    public OrderAddressApplicationRequest toApplicationRequest(){
        return OrderAddressApplicationRequest.builder()
                .street(this.street)
                .postalCode(this.postalCode)
                .city(this.city)
                .build();
    }
}
