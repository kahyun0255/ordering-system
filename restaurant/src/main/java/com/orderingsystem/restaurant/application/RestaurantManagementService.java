package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.InvalidCredentialsException;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.RestaurantUpdateOutboxHelper;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantCreateService;
import com.orderingsystem.restaurant.domain.service.RestaurantCreateService.RestaurantCreateResult;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantManagementService {

    private final OwnerRepository ownerRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantOwnershipRepository restaurantOwnershipRepository;
    private final RestaurantUpdateOutboxHelper restaurantUpdateOutboxHelper;
    private final RestaurantCreateService restaurantCreateService;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional
    public CreateRestaurantResponse createRestaurant(CreateRestaurantApplicationRequest request) {
        Owner owner = findOwner(request.getOwnerId());

        RestaurantCreateResult restaurantCreateResult = restaurantCreateService.create(owner, request.getName());

        restaurantRepository.save(restaurantCreateResult.getRestaurant());
        restaurantOwnershipRepository.save(restaurantCreateResult.getRestaurantOwnership());

        restaurantUpdateOutboxHelper.saveRestaurantUpdateOutboxMessage(
                restaurantDataMapper.createdRestaurantEventToRestaurantUpdateEventPayload(restaurantCreateResult.getCreatedRestaurantEvent()),
                OutboxStatus.STARTED,
                UUID.randomUUID()
        );

        return CreateRestaurantResponse.builder()
                .restaurantId(restaurantCreateResult.getRestaurant().getRestaurantId())
                .message("레스토랑이 성공적으로 생성되었습니다.")
                .build();
    }

    private Owner findOwner(UUID ownerId) {
        Optional<Owner> owner = ownerRepository.findById(ownerId);

        if (owner.isEmpty()) {
            log.warn("레스토랑 오너 정보를 찾을 수 없습니다. Owner Id : {}", ownerId);
            throw new InvalidCredentialsException("레스토랑 오너 정보를 찾을 수 없습니다.");
        }

        return owner.get();
    }
}
