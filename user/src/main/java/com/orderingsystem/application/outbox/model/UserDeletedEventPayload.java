package com.orderingsystem.application.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class UserDeletedEventPayload extends UserEventPayload {

    @JsonProperty
    private String username;
    @JsonProperty
    private String type;

}
