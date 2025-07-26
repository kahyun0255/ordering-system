package com.orderingsystem.restaurant.domain.repository;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderApprovalRepository extends JpaRepository<OrderApproval, UUID> {
    boolean existsByOrderIdAndRestaurantIdAndStatus(UUID orderId, UUID restaurantId, OrderApprovalStatus status);
}
