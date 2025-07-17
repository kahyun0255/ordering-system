package com.orderingsystem.order.infra.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "order-topic")
@Data
public class OrderMessageConfigData {

    private String paymentRequestTopicName;
    private String paymentResponseTopicName;

}
