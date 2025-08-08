package com.orderingsystem.order.infra.kafka.message;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CustomerDebeziumMessage {

    private PaymentResponseDebeziumMessage.Payload before;
    private PaymentResponseDebeziumMessage.Payload after;
    private String op;

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Payload{
        private String id;
        private Long createdAt;
        private String username;
    }

}
