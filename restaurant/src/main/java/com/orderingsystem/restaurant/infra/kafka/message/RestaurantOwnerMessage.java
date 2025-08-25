package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.restaurant.application.dto.request.RestaurantOwnerApplicationRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantOwnerMessage {

    private UUID id;
    private String username;
    private Instant createdAt;
    private String type;

    public RestaurantOwnerApplicationRequest toRestaurantOwnerApplicationRequest(){
        return RestaurantOwnerApplicationRequest.builder()
                .id(this.id)
                .username(this.username)
                .createdAt(this.createdAt)
                .build();

    }
}
