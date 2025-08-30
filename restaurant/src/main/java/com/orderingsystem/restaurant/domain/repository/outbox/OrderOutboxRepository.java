package com.orderingsystem.restaurant.domain.repository.outbox;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

    Optional<List<OrderOutbox>> findByType(String type);

    boolean existsByTypeAndSagaIdAndOrderApprovalStatus(String type, UUID sagaId,
                                                                       OrderApprovalStatus orderApprovalStatus);

    @Modifying
    @Query(value = "DELETE FROM order_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);
}
