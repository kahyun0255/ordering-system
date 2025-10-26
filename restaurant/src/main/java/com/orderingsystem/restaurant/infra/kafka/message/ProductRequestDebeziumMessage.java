package com.orderingsystem.restaurant.infra.kafka.message;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductRequestDebeziumMessage {

    private Payload before;
    private Payload after;
    private String op;

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Payload{
        private String id;
        private String sagaId;
        private String type;
        private String payload;
        private String orderStatus;
        private String sagaStatus;
        private Long version;
        private Long createdAt;
        private Long processedAt;
    }
}
