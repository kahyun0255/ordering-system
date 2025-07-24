package com.orderingsystem.restaurant.domain.repository.outbox;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

    Optional<OrderOutbox> findByTypeAndSagaIdAndOutboxStatus(String type, UUID sagaId, OutboxStatus outboxStatus);

    Optional<List<OrderOutbox>> findByTypeAndOutboxStatus(String orderSagaName, OutboxStatus outboxStatus);

    void deleteAllByTypeAndOutboxStatus(String orderSagaName, OutboxStatus outboxStatus);
}
