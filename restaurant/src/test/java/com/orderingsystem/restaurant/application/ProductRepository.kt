package com.orderingsystem.restaurant.application

import com.orderingsystem.restaurant.domain.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {
}