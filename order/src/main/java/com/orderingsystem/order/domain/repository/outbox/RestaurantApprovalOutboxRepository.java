package com.orderingsystem.order.domain.repository.outbox;

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

    Optional<List<RestaurantApprovalOutbox>> findByTypeAndOutboxStatusAndSagaStatus(String orderSagaName,
                                                                                    OutboxStatus outboxStatus,
                                                                                    SagaStatus sagaStatus);

    Optional<List<RestaurantApprovalOutbox>> findByTypeAndOutboxStatusAndSagaStatusIn(String orderSagaName,
                                                                                      OutboxStatus outboxStatus,
                                                                                      List<SagaStatus> list);

    void deleteAllByTypeAndOutboxStatusAndSagaStatusIn(String orderSagaName, OutboxStatus outboxStatus,
                                                       List<SagaStatus> list);
}
