package com.orderingsystem.restaurant.application.dto.response;

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
public class RestaurantInfoResponse {

    private UUID restaurantId;
    private boolean active;
    private List<ProductInfoResponse> products;

}
