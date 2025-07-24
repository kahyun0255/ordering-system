package com.orderingsystem.payment.domain.repository.outbox;

import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
    Optional<OrderOutbox> findByTypeAndSagaIdAndPaymentStatusAndOutboxStatus(String type, UUID sagaId,
                                                                             PaymentStatus paymentStatus,
                                                                             OutboxStatus outboxStatus);

    Optional<List<OrderOutbox>> findByTypeAndOutboxStatus(String orderSagaName, OutboxStatus outboxStatus);

    void deleteAllByTypeAndOutboxStatus(String orderSagaName, OutboxStatus outboxStatus);
}
