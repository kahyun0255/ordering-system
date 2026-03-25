package com.orderingsystem.order.application.outbox.coupon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OrderCouponEventPayload {

    @JsonProperty
    private String orderId;
    @JsonProperty
    private String customerId;
    @JsonProperty
    private String sagaId;
    @JsonProperty
    private List<String> issuedCouponId;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private List<String> failureMessage;
    @JsonProperty
    private String action;

}
