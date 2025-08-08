package com.orderingsystem.application.outbox.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CustomerEventPayload {

    @JsonProperty
    private String id;
    @JsonProperty
    private String username;
    @JsonProperty
    private ZonedDateTime createdAt;

}
