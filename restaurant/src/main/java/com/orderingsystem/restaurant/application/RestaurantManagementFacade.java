package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.domain.status.OutboxEventOperation;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.restaruantupdate.RestaurantUpdateOutboxHelper;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.event.restaruant.UpdatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
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
    private final RestaurantUpdateService restaurantUpdateService;

    @Transactional
    public CreateRestaurantResponse createRestaurant(CreateRestaurantApplicationRequest request) {
        Owner owner = restaurantAccessValidatorService.findOwner(request.getOwnerId());
        CreatedRestaurantEvent createdRestaurantEvent = restaurantCreateService.create(request, owner);

        restaurantUpdateOutboxHelper.saveRestaurantUpdateOutboxMessage(
                restaurantDataMapper.restaurantEventToRestaurantUpdateEventPayload(
                        createdRestaurantEvent, OutboxEventOperation.INSERT),
                OutboxStatus.STARTED,
                UUID.randomUUID()
        );

        log.info("레스토랑 생성 완료. Restaurant Id : {}, Owner Id : {}",
                createdRestaurantEvent.getRestaurant().getRestaurantId(), request.getOwnerId());

        return CreateRestaurantResponse.builder()
                .restaurantId(createdRestaurantEvent.getRestaurant().getRestaurantId())
                .message("레스토랑이 성공적으로 생성되었습니다.")
                .build();
    }

    @Transactional
    public UpdateRestaurantResponse updateRestaurant(
            UpdateRestaurantApplicationRequest request, UUID restaurantId, UUID restaurantOwnerId) {
        Owner owner = restaurantAccessValidatorService.findOwner(restaurantOwnerId);
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        restaurantAccessValidatorService.validateRestaurantOwnership(owner, restaurant);

        UpdatedRestaurantEvent updatedRestaurantEvent = restaurantUpdateService.update(request, restaurant);

        if (updatedRestaurantEvent == null) {
            return UpdateRestaurantResponse.builder()
                    .restaurantId(restaurantId)
                    .name(restaurant.getName())
                    .status(restaurant.getStatus())
                    .message("변경된 내용이 없습니다.")
                    .build();
        }

        restaurantUpdateOutboxHelper.saveRestaurantUpdateOutboxMessage(
                restaurantDataMapper.restaurantEventToRestaurantUpdateEventPayload(
                        updatedRestaurantEvent, OutboxEventOperation.UPDATE),
                OutboxStatus.STARTED,
                UUID.randomUUID()
        );

        log.info("레스토랑 업데이트 완료. Restaurant Id : {}, Owner Id : {}, name : {}, status : {}",
                updatedRestaurantEvent.getRestaurant().getRestaurantId(), restaurantOwnerId,
                updatedRestaurantEvent.getRestaurant().getName(), updatedRestaurantEvent.getRestaurant().getStatus());

        return UpdateRestaurantResponse.builder()
                .restaurantId(restaurantId)
                .name(updatedRestaurantEvent.getRestaurant().getName())
                .status(updatedRestaurantEvent.getRestaurant().getStatus())
                .message("성공적으로 변경 되었습니다.")
                .build();
    }

    @Transactional
    public void deleteRestaurant(UUID restaurantId, UUID restaurantOwnerId) {
        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        Owner owner = restaurantAccessValidatorService.findOwner(restaurantOwnerId);
        restaurantAccessValidatorService.validateRestaurantOwnership(owner, restaurant);

        restaurant.delete();
    }
}
