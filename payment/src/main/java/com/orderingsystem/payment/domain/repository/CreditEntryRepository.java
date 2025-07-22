package com.orderingsystem.payment.domain.repository;

import com.orderingsystem.payment.domain.model.CreditEntry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditEntryRepository extends JpaRepository<CreditEntry, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CreditEntry> findByCustomerId(UUID customerId);
}
