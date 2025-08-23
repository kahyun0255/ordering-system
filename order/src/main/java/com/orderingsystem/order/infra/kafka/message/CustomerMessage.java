package com.orderingsystem.order.infra.kafka.message;

import com.orderingsystem.order.application.dto.request.CreateCustomerApplicationRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CustomerMessage {

    private UUID id;
    private String username;
    private Instant createdAt;
    private String type;

    public CreateCustomerApplicationRequest toCreateCustomerApplicationRequest(){
        return CreateCustomerApplicationRequest.builder()
                .id(this.id)
                .username(this.username)
                .createdAt(this.createdAt)
                .build();
    }

}
