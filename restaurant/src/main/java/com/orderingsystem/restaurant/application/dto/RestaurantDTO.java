package com.orderingsystem.restaurant.application.dto;

import com.orderingsystem.restaurant.domain.model.Product;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RestaurantDTO {

    private final UUID restaurantId;
    private final List<Product> products;
    private boolean active;

}
