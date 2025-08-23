package com.orderingsystem.order.application.dto.request;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantUpdateApplicationRequest {
    private String restaurantId;
    private Instant createdAt;
    private String name;
    private boolean active;
    private String type;
}
