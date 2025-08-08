package com.orderingsystem.order.domain.repository.restaurant;

import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.model.restaurant.RestaurantInfoView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantReadRepository extends JpaRepository<Restaurant, UUID> {

    @Query("""
                SELECT 
                    r.restaurantId AS restaurantId,
                    r.name AS restaurantName,
                    r.active AS restaurantActive,
                    p.productId AS productId,
                    p.name AS productName,
                    p.price AS productPrice,
                    p.available AS productAvailable
                FROM Restaurant r
                JOIN RestaurantProduct rp ON r.restaurantId = rp.restaurantId
                JOIN Product p ON p.productId = rp.productId
                WHERE r.restaurantId = :restaurantId
                  AND p.productId IN :productIds
            """)
    List<RestaurantInfoView> findRestaurantInfo(UUID restaurantId, List<UUID> productIds);

}
