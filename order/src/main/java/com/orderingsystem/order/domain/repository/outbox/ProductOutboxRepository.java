package com.orderingsystem.order.domain.repository.outbox;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.ProductOutbox;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductOutboxRepository extends JpaRepository<ProductOutbox, UUID> {

    @Modifying
    @Query(value = "DELETE FROM product_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);

    boolean existsByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId, SagaStatus sagaStatus);

    Optional<ProductOutbox> findByTypeAndSagaIdAndSagaStatusIn(String type, UUID sagaId, List<SagaStatus> list);

    Optional<ProductOutbox> findBySagaIdAndSagaStatus(UUID sagaId, SagaStatus sagaStatus);

}
