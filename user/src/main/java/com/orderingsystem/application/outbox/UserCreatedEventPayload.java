package com.orderingsystem.application.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserCreatedEventPayload {

    @JsonProperty
    private String id;
    @JsonProperty
    private String username;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String type;

}
