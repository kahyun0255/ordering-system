package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.order.application.dto.request.RestaurantUpdateApplicationRequest;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantUpdateMessage {
    private String restaurantId;
    private Instant createdAt;
    private String name;
    private boolean active;
    private String type;

    public RestaurantUpdateApplicationRequest toRestaurantUpdateApplicationRequest(){
        return RestaurantUpdateApplicationRequest.builder()
                .restaurantId(this.restaurantId)
                .createdAt(this.createdAt)
                .name(this.name)
                .active(this.active)
                .type(this.type)
                .build();
    }
}
