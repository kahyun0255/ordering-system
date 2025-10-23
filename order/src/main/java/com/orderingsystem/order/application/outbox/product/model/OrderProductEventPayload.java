package com.orderingsystem.order.application.outbox.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OrderProductEventPayload {

    @JsonProperty
    private String orderId;
    @JsonProperty
    private String sagaId;
    @JsonProperty
    private String restaurantId;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String type;
    @JsonProperty
    private List<String> failureMessage;
    @JsonProperty
    private List<OrderProductEventProduct> products;

}
