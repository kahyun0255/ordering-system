package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantOwnershipRepository extends JpaRepository<RestaurantOwnership, Long> {
    List<RestaurantOwnership> findByOwnerId(UUID ownerId);

    Optional<RestaurantOwnership> findByOwnerIdAndRestaurantId(UUID ownerId, UUID restaurantId);
}
