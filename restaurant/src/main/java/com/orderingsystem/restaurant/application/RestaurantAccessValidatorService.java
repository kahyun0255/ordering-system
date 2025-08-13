package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
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
    private final RestaurantRepository restaurantRepository;
    private final RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Transactional(readOnly = true)
    public Owner findOwner(UUID ownerId) {
        Optional<Owner> owner = ownerRepository.findById(ownerId);

        if (owner.isEmpty()) {
            log.warn("레스토랑 오너 정보를 찾을 수 없습니다. Owner Id : {}", ownerId);
            throw new AccessDeniedException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }

        return owner.get();
    }

    @Transactional(readOnly = true)
    public Restaurant findRestaurant(UUID restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);

        if (restaurant.isEmpty()) {
            log.warn("레스토랑 정보를 찾을 수 없습니다. Restaurant Id : {}", restaurantId);
            throw new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
        }

        return restaurant.get();
    }

    public void validateRestaurantOwnership(Owner owner, Restaurant restaurant) {
        Optional<RestaurantOwnership> restaurantOwnerships = restaurantOwnershipRepository.findByOwnerIdAndRestaurantId(
                owner.getUserId(), restaurant.getRestaurantId());

        if (restaurantOwnerships.isEmpty()) {
            log.warn("레스토랑 정보를 수정 할 권한이 없습니다. OwnerId : {}, Restaurant Id : {}",
                    owner.getUserId(), restaurant.getRestaurantId());
            throw new AccessDeniedException("레스토랑 정보를 수정 할 권한이 없습니다.");
        }
    }
}
