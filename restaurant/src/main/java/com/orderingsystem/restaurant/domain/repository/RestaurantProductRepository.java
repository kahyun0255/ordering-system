package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantProductRepository extends JpaRepository<RestaurantProduct, UUID> {

    List<RestaurantProduct> findByRestaurantIdAndProductIdIn(UUID restaurantId, List<UUID> productIds);

    List<RestaurantProduct> findAllByRestaurantId(UUID restaurantId);

    Optional<RestaurantProduct> findByRestaurantIdAndProductId(UUID restaurantId, UUID productId);

}
