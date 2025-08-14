package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.RESTAURANT_CREATE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.CreateRestaurantResponse;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.outbox.RestaurantUpdateOutbox;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RestaurantManagementFacadeCreateTest extends ApplicationTestSupport {

    @Autowired
    private RestaurantManagementFacade restaurantManagementFacade;

    private final UUID ownerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantUpdateOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑을 성공적으로 생성한다.")
    @Test
    void createRestaurantSuccessfully() {
        //given
        ownerRepository.save(Owner.builder()
                .name("name")
                .userId(ownerId)
                .build());

        CreateRestaurantApplicationRequest request = CreateRestaurantApplicationRequest.builder()
                .ownerId(ownerId)
                .name("restaurant1")
                .build();

        //when
        CreateRestaurantResponse response = restaurantManagementFacade.createRestaurant(request);

        //then
        assertThat(response.getRestaurantId()).isNotNull();
        assertThat(response.getMessage()).isEqualTo("레스토랑이 성공적으로 생성되었습니다.");

        Optional<Restaurant> savedRestaurant =
                restaurantRepository.findById(response.getRestaurantId());
        assertThat(savedRestaurant).isPresent();
        assertThat(savedRestaurant.get().getActive()).isTrue();
        assertThat(savedRestaurant.get().getName()).isEqualTo(request.getName());

        List<RestaurantOwnership> savedRestaurantOwnership = restaurantOwnershipRepository.findByOwnerId(ownerId);
        assertThat(savedRestaurantOwnership).hasSize(1)
                .extracting("restaurantId", "ownerId")
                .containsExactlyInAnyOrder(tuple(response.getRestaurantId(), ownerId));

        List<RestaurantUpdateOutbox> outbox = restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(
                RESTAURANT_CREATE_NAME, OutboxStatus.STARTED);
        assertThat(outbox).isNotEmpty();
    }

    @DisplayName("레스토랑 오너 정보가 없으면 레스토랑 저장에 실패하고, 예외가 발생한다.")
    @Test
    void failToSaveRestaurant_whenOwnerInfoIsMissing() {
        //given
        CreateRestaurantApplicationRequest request = CreateRestaurantApplicationRequest.builder()
                .ownerId(ownerId)
                .name("restaurant1")
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.createRestaurant(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 오너 정보를 찾을 수 없습니다.");

        assertThat(restaurantRepository.count()).isEqualTo(0L);
        assertThat(restaurantOwnershipRepository.count()).isEqualTo(0L);
    }

}
