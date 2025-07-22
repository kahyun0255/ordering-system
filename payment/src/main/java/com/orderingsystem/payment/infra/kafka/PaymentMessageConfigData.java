package com.orderingsystem.payment.infra.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "payment-topic")
public class PaymentMessageConfigData {

    private String paymentRequestTopicName;
    private String paymentResponseTopicName;

}
