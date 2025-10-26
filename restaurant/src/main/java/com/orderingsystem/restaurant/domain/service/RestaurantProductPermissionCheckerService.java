package com.orderingsystem.restaurant.domain.service;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import org.springframework.stereotype.Service;

@Service
public class RestaurantProductPermissionCheckerService {

    public boolean canManageProduct(Restaurant restaurant) {
        return restaurant.getStatus().canManageProduct();
    }

}
