package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantCreateService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Transactional
    public CreatedRestaurantEvent create(CreateRestaurantApplicationRequest request, Owner owner) {
        UUID restaurantId = UUID.randomUUID();

        Restaurant savedRestaurant = restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name(request.getName())
                .active(true)
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(owner.getUserId())
                .restaurantId(restaurantId)
                .build());

        return new CreatedRestaurantEvent(savedRestaurant, ZonedDateTime.now());
    }

}
