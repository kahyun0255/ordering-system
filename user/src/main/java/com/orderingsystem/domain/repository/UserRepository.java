package com.orderingsystem.domain.repository;

import com.orderingsystem.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsById(String id);

    Optional<User> findById(String id);
}
