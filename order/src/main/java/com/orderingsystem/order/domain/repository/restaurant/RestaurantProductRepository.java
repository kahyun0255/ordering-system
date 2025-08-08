package com.orderingsystem.order.domain.repository.restaurant;

import com.orderingsystem.order.domain.model.restaurant.RestaurantProduct;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantProductRepository extends JpaRepository<RestaurantProduct, UUID> {
}
