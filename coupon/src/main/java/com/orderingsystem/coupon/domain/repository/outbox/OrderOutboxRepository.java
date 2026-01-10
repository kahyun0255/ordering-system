package com.orderingsystem.coupon.domain.repository.outbox;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.coupon.domain.model.outbox.OrderOutbox;
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

    Optional<OrderOutbox> findByTypeAndSagaIdAndSagaStatusIn(String type, UUID sagaId,
                                                             List<SagaStatus> sagaStatuses);

    boolean existsByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId, SagaStatus sagaStatus);

    @Modifying
    @Query(value = "DELETE FROM coupon_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);

    Optional<OrderOutbox> findBySagaIdAndSagaStatus(UUID sagaId, SagaStatus sagaStatus);

}
