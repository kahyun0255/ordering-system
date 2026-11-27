package com.orderingsystem.payment.domain.repository.outbox;

import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
    Optional<OrderOutbox> findByTypeAndSagaIdAndPaymentStatus(String type, UUID sagaId,
                                                              PaymentStatus paymentStatus);

    boolean existsByTypeAndSagaIdAndPaymentStatus(String type, UUID sagaId, PaymentStatus paymentStatus);

    boolean existsByTypeAndSagaIdAndPaymentStatusIn(String type, UUID sagaId, Collection<PaymentStatus> paymentStatuses);

    @Modifying
    @Query(value = "DELETE FROM order_outbox WHERE created_at < :threshold", nativeQuery = true)
    int deleteOlderThan(ZonedDateTime threshold);

}
