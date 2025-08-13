package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.RESTAURANT_CREATE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.event.restaruant.CreatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.RestaurantUpdateOutboxRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RestaurantCreateServiceTest {

    @Autowired
    private RestaurantCreateService restaurantCreateService;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantUpdateOutboxRepository restaurantUpdateOutboxRepository;

    private final UUID ownerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantUpdateOutboxRepository.deleteAllInBatch();
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

        Optional<List<RestaurantUpdateOutbox>> outbox = restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(
                RESTAURANT_CREATE_NAME, OutboxStatus.STARTED);
        assertThat(outbox).isPresent();
    }

}
