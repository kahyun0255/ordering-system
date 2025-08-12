package com.orderingsystem.restaurant.application.outbox.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderEventPayload {

    @JsonProperty
    private String orderId;
    @JsonProperty
    private String restaurantId;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String orderApprovalStatus;
    @JsonProperty
    private List<String> failureMessages;

}
