package com.orderingsystem.restaurant.domain.repository.outbox;

import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantUpdateOutboxRepository extends JpaRepository<RestaurantUpdateOutbox, UUID> {

    boolean existsByTypeAndEventId(String type, UUID eventId);

    List<RestaurantUpdateOutbox> findByType(String type);

    void deleteAllByType(String type);
}
