package com.orderingsystem.order.domain.repository;

import com.orderingsystem.order.domain.model.OrderAddress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAddressRepository extends JpaRepository<OrderAddress, UUID> {
}