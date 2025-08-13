package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantAccessValidatorService {

    private final OwnerRepository ownerRepository;

    @Transactional(readOnly = true)
    public Owner findOwner(UUID ownerId) {
        Optional<Owner> owner = ownerRepository.findById(ownerId);

        if (owner.isEmpty()) {
            log.warn("레스토랑 오너 정보를 찾을 수 없습니다. Owner Id : {}", ownerId);
            throw new InvalidCredentialsException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }

        return owner.get();
    }

}
