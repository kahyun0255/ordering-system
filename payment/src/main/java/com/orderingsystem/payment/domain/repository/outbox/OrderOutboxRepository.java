package com.orderingsystem.payment.domain.repository.outbox;

import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.payment.domain.model.outbox.OrderOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
    Optional<OrderOutbox> findByTypeAndSagaIdAndPaymentStatus(String type, UUID sagaId,
                                                              PaymentStatus paymentStatus);

    Optional<List<OrderOutbox>> findByType(String orderSagaName);

    void deleteAllByType(String orderSagaName);

    boolean existsByTypeAndSagaIdAndPaymentStatus(String type, UUID sagaId, PaymentStatus paymentStatus);
}
