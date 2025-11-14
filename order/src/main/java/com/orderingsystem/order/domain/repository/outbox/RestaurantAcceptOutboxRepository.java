package com.orderingsystem.order.domain.repository.outbox;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantAcceptOutboxRepository extends JpaRepository<RestaurantAcceptOutbox, UUID> {

    Optional<RestaurantAcceptOutbox> findByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId,
                                                                        SagaStatus sagaStatus);

    boolean existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(String type, UUID sagaId, OrderStatus orderStatus,
                                                             SagaStatus sagaStatus);

    @Modifying
    @Query(value = "DELETE FROM restaurant_accept_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);

}
