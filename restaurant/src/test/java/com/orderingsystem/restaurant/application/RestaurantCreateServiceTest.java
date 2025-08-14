package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RestaurantCreateServiceTest extends ApplicationTestSupport {

    @Autowired
    private RestaurantCreateService restaurantCreateService;

    private final UUID ownerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑이 성공적으로 저장된다.")
    @Test
    void saveRestaurantSuccessfully() {
        //given
        Owner owner = Owner.builder()
                .name("name")
                .userId(ownerId)
                .build();

        ownerRepository.save(owner);

        CreateRestaurantApplicationRequest request = CreateRestaurantApplicationRequest.builder()
                .ownerId(ownerId)
                .name("restaurant1")
                .build();

        //when
        CreatedRestaurantEvent createdRestaurantEvent = restaurantCreateService.create(request, owner);

        //then
        assertThat(createdRestaurantEvent.getRestaurant().getRestaurantId()).isNotNull();
        assertThat(createdRestaurantEvent.getRestaurant().getName()).isEqualTo(request.getName());

        Optional<Restaurant> savedRestaurant =
                restaurantRepository.findById(createdRestaurantEvent.getRestaurant().getRestaurantId());
        assertThat(savedRestaurant).isPresent();
        assertThat(savedRestaurant.get().getActive()).isTrue();
        assertThat(savedRestaurant.get().getName()).isEqualTo(request.getName());

        List<RestaurantOwnership> savedRestaurantOwnership = restaurantOwnershipRepository.findByOwnerId(ownerId);
        assertThat(savedRestaurantOwnership).hasSize(1)
                .extracting("restaurantId", "ownerId")
                .containsExactlyInAnyOrder(tuple(createdRestaurantEvent.getRestaurant().getRestaurantId(), ownerId));
    }

}
