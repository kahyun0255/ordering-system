package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.restaurant.domain.model.RestaurantOwner;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantOwnerRepository extends JpaRepository<RestaurantOwner, UUID> {
}
