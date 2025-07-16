package com.orderingsystem.order.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class OrderAddressApplicationRequest {

    private final String street;
    private final String postalCode;
    private final String city;

}
