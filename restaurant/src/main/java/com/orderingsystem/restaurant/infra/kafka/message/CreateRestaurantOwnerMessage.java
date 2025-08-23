package com.orderingsystem.restaurant.infra.kafka.message;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantOwnerApplicationRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreateRestaurantOwnerMessage {

    private UUID id;
    private String username;
    private Instant createdAt;
    private String type;

    public CreateRestaurantOwnerApplicationRequest toCreateRestaurantOwnerApplicationRequest(){
        return CreateRestaurantOwnerApplicationRequest.builder()
                .id(this.id)
                .username(this.username)
                .createdAt(this.createdAt)
                .build();

    }
}
