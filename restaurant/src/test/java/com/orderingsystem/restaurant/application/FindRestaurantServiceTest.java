package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FindRestaurantServiceTest extends ApplicationTestSupport {

    @Autowired
    private FindRestaurantService findRestaurantService;

    @AfterEach
    void tearDown() {
        restaurantRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑을 상세조회한다.")
    @Test
    void getRestaurantDetailsSuccessfully() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        //when
        FindRestaurantResponse response = findRestaurantService.findRestaurant(restaurant.getRestaurantId());

        //then
        assertThat(response.getName()).isEqualTo(restaurant.getName());
        assertThat(response.getStatus()).isEqualTo(restaurant.getStatus());
    }

    @DisplayName("레스토랑이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantDoesNotExist() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
                .build();

        //when, then
        assertThatThrownBy(() -> findRestaurantService.findRestaurant(restaurant.getRestaurantId()))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");

    }
}
