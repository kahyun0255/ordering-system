package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
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

}
