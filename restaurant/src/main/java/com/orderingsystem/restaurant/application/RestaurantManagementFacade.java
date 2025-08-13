package com.orderingsystem.restaurant.application;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.RestaurantUpdateOutboxHelper;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantManagementFacade {

    private final RestaurantCreateService restaurantCreateService;
    private final RestaurantAccessValidatorService restaurantAccessValidatorService;
    private final RestaurantUpdateOutboxHelper restaurantUpdateOutboxHelper;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional
    public CreateRestaurantResponse createRestaurant(CreateRestaurantApplicationRequest request) {
        Owner owner = restaurantAccessValidatorService.findOwner(request.getOwnerId());
        CreatedRestaurantEvent createdRestaurantEvent = restaurantCreateService.create(request, owner);

        restaurantUpdateOutboxHelper.saveRestaurantUpdateOutboxMessage(
                restaurantDataMapper.createdRestaurantEventToRestaurantUpdateEventPayload(createdRestaurantEvent),
                OutboxStatus.STARTED,
                UUID.randomUUID()
        );

        return CreateRestaurantResponse.builder()
                .restaurantId(createdRestaurantEvent.getRestaurant().getRestaurantId())
                .message("레스토랑이 성공적으로 생성되었습니다.")
                .build();
    }

}
