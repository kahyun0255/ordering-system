package com.orderingsystem.order.application.outbox.restaurant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantApprovalEventProduct {

    @JsonProperty
    private String id;
    @JsonProperty
    private Integer quantity;

}
