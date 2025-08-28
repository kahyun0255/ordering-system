package com.orderingsystem.restaurant.domain.repository.outbox;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

    Optional<List<OrderOutbox>> findByType(String type);

    void deleteAllByType(String type);

    boolean existsByTypeAndSagaIdAndOrderApprovalStatus(String type, UUID sagaId,
                                                                       OrderApprovalStatus orderApprovalStatus);

}
