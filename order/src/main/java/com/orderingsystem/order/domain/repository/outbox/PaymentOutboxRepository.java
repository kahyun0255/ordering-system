package com.orderingsystem.order.domain.repository.outbox;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
    Optional<List<PaymentOutbox>> findByTypeAndOutboxStatusAndSagaStatusIn(String type, OutboxStatus outboxStatus,
                                                                           List<SagaStatus> sagaStatuses);

    void deleteByTypeAndOutboxStatusAndSagaStatusIn(String orderSagaName, OutboxStatus outboxStatus, List<SagaStatus> sagaStatus);
}