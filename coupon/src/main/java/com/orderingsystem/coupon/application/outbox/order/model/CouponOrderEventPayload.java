package com.orderingsystem.coupon.application.outbox.order.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponOrderEventPayload {

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
    private int updatedCount;

}
