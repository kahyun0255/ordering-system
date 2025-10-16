package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.restaurant.domain.model.Product;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
                SELECT p
                FROM Product p
                JOIN RestaurantProduct rp ON p.productId = rp.productId
                WHERE rp.restaurantId = :restaurantId AND p.available = true
            """)
    Page<Product> findByRestaurantId(UUID restaurantId, Pageable pageable);

}
