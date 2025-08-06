package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.restaurant.domain.model.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
