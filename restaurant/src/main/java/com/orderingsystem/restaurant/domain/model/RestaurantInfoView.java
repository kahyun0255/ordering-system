package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.Money;
import java.util.UUID;

public interface RestaurantInfoView {
    UUID getRestaurantId();
    String getRestaurantName();
    RestaurantStatus getRestaurantStaus();
    UUID getProductId();
    String getProductName();
    Money getProductPrice();
    Boolean getProductAvailable();
}
