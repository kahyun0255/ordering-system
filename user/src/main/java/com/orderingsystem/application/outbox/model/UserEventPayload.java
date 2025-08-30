package com.orderingsystem.application.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class UserEventPayload {

    @JsonProperty
    private String id;

}
