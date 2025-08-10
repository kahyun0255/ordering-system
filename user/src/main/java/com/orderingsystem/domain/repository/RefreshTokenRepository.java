package com.orderingsystem.domain.repository;

import com.orderingsystem.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenAndUserId(String token, UUID userId);

    Optional<RefreshToken> findByUserId(UUID id);
}
