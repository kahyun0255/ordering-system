package com.orderingsystem.restaurant.domain.repository.outbox;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantUpdateOutboxRepository extends JpaRepository<RestaurantUpdateOutbox, UUID> {

    boolean existsByTypeAndEventIdAndOutboxStatus(String type, UUID eventId, OutboxStatus outboxStatus);

    Optional<List<RestaurantUpdateOutbox>> findByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);

    void deleteAllByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus);
}
