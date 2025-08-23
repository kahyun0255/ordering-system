package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import java.util.UUID;

public interface RestaurantApi {

    void validRestaurantAndProducts(CreateOrderApplicationRequest createOrderRequest, UUID sagaId);

}
