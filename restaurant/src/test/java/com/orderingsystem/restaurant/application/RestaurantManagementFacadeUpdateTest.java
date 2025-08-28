package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.RESTAURANT_CREATE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RestaurantManagementFacadeUpdateTest extends ApplicationTestSupport {

    @Autowired
    private RestaurantManagementFacade restaurantManagementFacade;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("레스토랑 오너")
                .build());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());
    }

    @AfterEach
    void tearDown() {
        restaurantRepository.deleteAllInBatch();
        ownerRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
        restaurantUpdateOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 이름 업데이트에 성공한다.")
    @Test
    void updateRestaurantNameSuccessfully() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when
        restaurantManagementFacade.updateRestaurant(request, restaurantId, ownerId);

        //then
        Optional<Restaurant> afterRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(afterRestaurant).isPresent();
        assertThat(afterRestaurant.get().getName()).isEqualTo(request.getName());

        assertThat(restaurantUpdateOutboxRepository.findByType(RESTAURANT_CREATE_NAME)).isNotEmpty();
    }

    @DisplayName("변경 사항이 없으면 변경된 내용이 없다는 메시지를 반환하고, outbox에 저장되지 않는다.")
    @Test
    void shouldNotUpdateWhenNoChanges() {
        //given
        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(beforeRestaurant.get().getName())
                .build();

        //when
        UpdateRestaurantResponse response = restaurantManagementFacade.updateRestaurant(request,
                restaurantId, ownerId);

        //then
        assertThat(response.getMessage()).isEqualTo("변경된 내용이 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByType(RESTAURANT_CREATE_NAME)).isEmpty();
    }

    @DisplayName("레스토랑 이름이 공백이라면 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantName_whenNameIsBlank() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(" ")
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.updateRestaurant(request, restaurantId, ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레스토랑 이름은 비어있을 수 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByType(RESTAURANT_CREATE_NAME)).isEmpty();
    }

    @DisplayName("레스토랑 오너가 아니면 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenNotOwner() {
        //given
        UUID notOwnerId = UUID.randomUUID();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, restaurantId, notOwnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 오너 정보를 찾을 수 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByType(RESTAURANT_CREATE_NAME)).isEmpty();
    }

    @DisplayName("레스토랑 정보를 찾을 수 없으면 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenRestaurantNotFound() {
        //given
        UUID notRestaurantId = UUID.randomUUID();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .build();

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, notRestaurantId, ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

    @DisplayName("해당 레스토랑을 수정 할 권한이 없으면 레스토랑 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenNotAuthorized() {
        //given
        UUID notOwnershipOwnerId = UUID.randomUUID();

        ownerRepository.save(Owner.builder()
                .userId(notOwnershipOwnerId)
                .name("레스토랑 오너")
                .build());

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .build();

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, restaurantId,
                        notOwnershipOwnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 정보를 수정 할 권한이 없습니다.");
    }

}
