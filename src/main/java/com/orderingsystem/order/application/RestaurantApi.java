package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.RestaurantInfo;
import java.util.List;
import java.util.UUID;

public interface RestaurantApi {

    RestaurantInfo getRestaurantInfo(UUID restaurantId, List<UUID> productIds);
}
