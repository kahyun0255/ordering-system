package com.orderingsystem.order.application.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantInfo {

    private UUID restaurantId;
    private String restaurantName;
    private boolean active;
    private List<ProductInfo> products;

}
