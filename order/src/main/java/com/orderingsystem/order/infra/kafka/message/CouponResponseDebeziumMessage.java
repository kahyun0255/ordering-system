package com.orderingsystem.order.infra.kafka.message;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CouponResponseDebeziumMessage {

    private Payload before;
    private Payload after;
    private String op;

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Payload {
        private String id;
        private Long createdAt;
        private String outboxStatus;
        private String payload;
        private Long processedAt;
        private String sagaId;
        private String type;
        private Long version;
    }

}
