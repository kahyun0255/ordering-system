package com.orderingsystem.payment.domain.repository;

import com.orderingsystem.payment.domain.model.CreditHistory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditHistoryRepository extends JpaRepository<CreditHistory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<List<CreditHistory>> findByCustomerId(UUID customerId);
}
