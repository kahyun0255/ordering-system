package com.orderingsystem.order.domain.repository;

import com.orderingsystem.order.domain.model.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByTrackingId(UUID trackingId);

    Optional<Order> findByTrackingIdAndCustomerId(UUID trackingId, UUID customerId);

}
