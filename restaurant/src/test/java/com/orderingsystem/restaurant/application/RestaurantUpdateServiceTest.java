package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
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

class RestaurantUpdateServiceTest extends ApplicationTestSupport {

    private final Restaurant restaurant = Restaurant.builder()
            .restaurantId(UUID.randomUUID())
            .name("기존 이름")
            .build();

    @BeforeEach
    void setUp() {
        UUID ownerId = UUID.randomUUID();
        ownerRepository.save(Owner.builder()
                .name("레스토랑 오너")
                .userId(ownerId)
                .build());

        restaurantRepository.save(restaurant);

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurant.getRestaurantId())
                .ownerId(ownerId)
                .build());
    }

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 이름 업데이트에 성공한다.")
    @Test
    void updateRestaurantNameSuccessfully() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("업데이트 이름")
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurant.getRestaurantId());
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isEqualTo(restaurant.getName());
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when
        restaurantUpdateService.update(request, beforeRestaurant.get());

        //then
        Optional<Restaurant> afterRestaurant = restaurantRepository.findById(restaurant.getRestaurantId());
        assertThat(afterRestaurant).isPresent();
        assertThat(afterRestaurant.get().getName()).isEqualTo(request.getName());
    }

    @DisplayName("변경할 이름이 공백이라면 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantName_whenNameIsBlank() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(" ")
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurant.getRestaurantId());
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isEqualTo(restaurant.getName());
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, beforeRestaurant.get()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레스토랑 이름은 비어있을 수 없습니다.");
    }

    @DisplayName("레스토랑 상태 변경에 성공한다.")
    @Test
    void shouldUpdateRestaurantStatusSuccessfully() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PRE_OPEN)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.ACTIVE)
                .build();

        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.PRE_OPEN);

        //when
        restaurantUpdateService.update(request, restaurant);

        //then
        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 현재 레스토랑 상태가 PENDING_APPROVAL이라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenStatusIsPendingApproval() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PENDING_APPROVAL)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.ACTIVE)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 현재 레스토랑 상태가 SUSPENDED라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenStatusIsSuspended() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.SUSPENDED)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.ACTIVE)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 현재 레스토랑 상태가 DELETED라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenStatusIsDeleted() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.ACTIVE)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 현재 레스토랑 상태가 PERM_CLOSED라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenStatusIsPermClosed() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PERM_CLOSED)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.ACTIVE)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 변경할 상태가 PENDING_APPROVAL 상태라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenTargetStatusIsPendingApproval() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.PENDING_APPROVAL)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 변경할 상태가 SUSPENDED 상태라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenTargetStatusIsSuspended() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

    @DisplayName("오너가 레스토랑 상태 변경을 요청하며, 변경할 상태가 DELETED 상태라면 변경이 불가능하다.")
    @Test
    void shouldThrowException_whenTargetStatusIsDeleted() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 이름")
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .status(RestaurantStatus.DELETED)
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantUpdateService.update(request, restaurant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태로 변경이 불가능합니다.");
    }

}
