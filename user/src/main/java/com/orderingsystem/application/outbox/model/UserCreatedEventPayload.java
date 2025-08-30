package com.orderingsystem.application.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class UserCreatedEventPayload extends UserEventPayload {

    @JsonProperty
    private String username;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String type;

}
