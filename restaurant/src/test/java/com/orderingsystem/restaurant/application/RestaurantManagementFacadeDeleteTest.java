package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RestaurantManagementFacadeDeleteTest extends ApplicationTestSupport {

    @Autowired
    private RestaurantManagementFacade restaurantManagementFacade;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build());

        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("오너 이름")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(ownerId)
                .build());
    }

    @AfterEach
    void tearDown() {
        restaurantRepository.deleteAllInBatch();
        ownerRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 삭제에 성공한다.")
    @Test
    void restaurantDelete() {
        //given
        assertThat(restaurantRepository.findById(restaurantId).get().getStatus()).isEqualTo(RestaurantStatus.ACTIVE);

        //when
        restaurantManagementFacade.deleteRestaurant(restaurantId, ownerId);

        //then
        assertThat(restaurantRepository.findById(restaurantId).get().getStatus()).isEqualTo(RestaurantStatus.DELETED);
    }

    @DisplayName("레스토랑 삭제 시, 레스토랑이 존재하지 않으면 삭제에 실패하며 예외가 발생한다.")
    @Test
    void shouldThrowException_whenDeletingNonExistentRestaurant() {
        //given
        UUID notRestaurantId = UUID.randomUUID();

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.deleteRestaurant(notRestaurantId, ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

    @DisplayName("레스토랑 삭제 시, 오너 정보가 존재하지 않으면 삭제에 실패하며 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOwnerInfoDoesNotExist() {
        //given
        UUID notOwnerId = UUID.randomUUID();

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.deleteRestaurant(restaurantId, notOwnerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 오너 정보를 찾을 수 없습니다.");
    }

    @DisplayName("레스토랑 삭제 시, 해당 레스토랑을 소유하고 있지 않으면 삭제에 실패하고 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotOwnRestaurant() {
        //given
        UUID notOwnerId = UUID.randomUUID();
        ownerRepository.save(Owner.builder()
                .userId(notOwnerId)
                .name("이름")
                .build());

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.deleteRestaurant(restaurantId, notOwnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 정보를 수정 할 권한이 없습니다.");
    }

}
