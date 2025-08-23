package com.orderingsystem.restaurant.infra.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "restaurant-topic")
@Data
public class RestaurantMessageConfigData {

    private String restaurantApprovalRequestTopicName;
    private String restaurantApprovalResponseTopicName;
    private String restaurantOwnerCreateTopicName;

}
