package com.orderingsystem.restaurant.application.dto.request;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantOwnerApplicationRequest {

    private UUID id;
    private String username;
    private Instant createdAt;

}
