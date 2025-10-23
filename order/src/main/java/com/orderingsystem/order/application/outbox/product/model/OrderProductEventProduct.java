package com.orderingsystem.order.application.outbox.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderProductEventProduct {

    @JsonProperty
    private String id;
    @JsonProperty
    private Integer quantity;

}
