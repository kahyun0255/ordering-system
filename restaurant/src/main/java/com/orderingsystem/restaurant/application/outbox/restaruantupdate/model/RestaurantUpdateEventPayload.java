package com.orderingsystem.restaurant.application.outbox.restaruantupdate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantUpdateEventPayload {

    @JsonProperty
    private String restaurantId;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String name;
    @JsonProperty
    private boolean active;
    @JsonProperty
    private String type;

}
