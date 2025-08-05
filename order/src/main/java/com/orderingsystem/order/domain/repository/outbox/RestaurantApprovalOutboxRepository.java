package com.orderingsystem.order.domain.repository.outbox;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantApprovalOutboxRepository extends JpaRepository<RestaurantApprovalOutbox, UUID> {

    Optional<List<RestaurantApprovalOutbox>> findByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                                                      OutboxStatus outboxStatus,
                                                                                      List<SagaStatus> sagaStatuses);

    void deleteAllByTypeAndOutboxStatusAndSagaStatusIn(String type, OutboxStatus outboxStatus,
                                                       List<SagaStatus> sagaStatuses);

    Optional<RestaurantApprovalOutbox> findByTypeAndSagaIdAndSagaStatus(String type, UUID sagaId,
                                                                        SagaStatus sagaStatus);

    boolean existsByTypeAndSagaIdAndOrderStatusAndSagaStatus(String type, UUID sagaId, OrderStatus orderStatus,
                                                             SagaStatus sagaStatus);
}
