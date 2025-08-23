package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.orderingsystem.restaurant.application.dto.response.FindAllRestaurantsResponse;
import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @DisplayName("ACTIVE, PRE_OPEN, TEMP_CLOSED인 기본 상태의 레스토랑 전체 조회에 성공한다.")
    @Test
    void shouldRetrieveAllRestaurantsSuccessfully() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 1")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PRE_OPEN)
                .name("레스토랑 2")
                .build();

        Restaurant restaurant3 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.TEMP_CLOSED)
                .name("레스토랑 3")
                .build();

        Restaurant restaurant4 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));

        //when
        Page<FindAllRestaurantsResponse> restaurants = findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                null);

        //then
        assertThat(restaurants.getContent()).hasSize(3)
                .extracting("restaurantId", "restaurantName", "status")
                .contains(
                        tuple(restaurant1.getRestaurantId(), restaurant1.getName(), restaurant1.getStatus()),
                        tuple(restaurant2.getRestaurantId(), restaurant2.getName(), restaurant2.getStatus()),
                        tuple(restaurant3.getRestaurantId(), restaurant3.getName(), restaurant3.getStatus())
                );
    }

    @DisplayName("ACTIVE 상태의 레스토랑 전체 조회에 성공한다.")
    @Test
    void shouldRetrieveAllActiveRestaurantsSuccessfully() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 1")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 2")
                .build();

        Restaurant restaurant3 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.TEMP_CLOSED)
                .name("레스토랑 3")
                .build();

        Restaurant restaurant4 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));

        //when
        Page<FindAllRestaurantsResponse> restaurants = findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.ACTIVE);

        //then
        assertThat(restaurants.getContent()).hasSize(2)
                .extracting("restaurantId", "restaurantName", "status")
                .contains(
                        tuple(restaurant1.getRestaurantId(), restaurant1.getName(), restaurant1.getStatus()),
                        tuple(restaurant2.getRestaurantId(), restaurant2.getName(), restaurant2.getStatus())
                );
    }

    @DisplayName("TEMP_CLOSED 상태의 레스토랑 전체 조회에 성공한다.")
    @Test
    void shouldRetrieveAllTempClosedRestaurantsSuccessfully() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 1")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.TEMP_CLOSED)
                .name("레스토랑 2")
                .build();

        Restaurant restaurant3 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.TEMP_CLOSED)
                .name("레스토랑 3")
                .build();

        Restaurant restaurant4 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));

        //when
        Page<FindAllRestaurantsResponse> restaurants = findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.TEMP_CLOSED);

        //then
        assertThat(restaurants.getContent()).hasSize(2)
                .extracting("restaurantId", "restaurantName", "status")
                .contains(
                        tuple(restaurant2.getRestaurantId(), restaurant2.getName(), restaurant2.getStatus()),
                        tuple(restaurant3.getRestaurantId(), restaurant3.getName(), restaurant3.getStatus())
                );
    }

    @DisplayName("PRE_OPEN 상태의 레스토랑 전체 조회에 성공한다.")
    @Test
    void shouldRetrieveAllPreOpenRestaurantsSuccessfully() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 1")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PRE_OPEN)
                .name("레스토랑 2")
                .build();

        Restaurant restaurant3 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.TEMP_CLOSED)
                .name("레스토랑 3")
                .build();

        Restaurant restaurant4 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));

        //when
        Page<FindAllRestaurantsResponse> restaurants = findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.PRE_OPEN);

        //then
        assertThat(restaurants.getContent()).hasSize(1)
                .extracting("restaurantId", "restaurantName", "status")
                .contains(
                        tuple(restaurant2.getRestaurantId(), restaurant2.getName(), restaurant2.getStatus())
                );
    }

    @DisplayName("PERM_CLOSED 상태의 레스토랑 전체 조회에 성공한다.")
    @Test
    void shouldRetrieveAllPermClosedRestaurantsSuccessfully() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑 1")
                .build();

        Restaurant restaurant2 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PRE_OPEN)
                .name("레스토랑 2")
                .build();

        Restaurant restaurant3 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PERM_CLOSED)
                .name("레스토랑 3")
                .build();

        Restaurant restaurant4 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));

        //when
        Page<FindAllRestaurantsResponse> restaurants = findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.PERM_CLOSED);

        //then
        assertThat(restaurants.getContent()).hasSize(1)
                .extracting("restaurantId", "restaurantName", "status")
                .contains(
                        tuple(restaurant3.getRestaurantId(), restaurant3.getName(), restaurant3.getStatus())
                );
    }

    @DisplayName("PENDING_APPROVAL 상태의 레스토랑은 조회하지 못하고 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantStatusIsPendingApproval() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PENDING_APPROVAL)
                .name("레스토랑 1")
                .build();

        restaurantRepository.save(restaurant1);

        //when, then
        assertThatThrownBy(()->findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.PENDING_APPROVAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태의 레스토랑 조회가 불가능합니다.");
    }

    @DisplayName("SUSPENDED 상태의 레스토랑은 조회하지 못하고 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantStatusIsSuspended() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.SUSPENDED)
                .name("레스토랑 1")
                .build();

        restaurantRepository.save(restaurant1);

        //when, then
        assertThatThrownBy(()->findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.SUSPENDED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태의 레스토랑 조회가 불가능합니다.");
    }

    @DisplayName("DELETED 상태의 레스토랑은 조회하지 못하고 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantStatusIsDeleted() {
        //given
        Restaurant restaurant1 = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.DELETED)
                .name("레스토랑 1")
                .build();

        restaurantRepository.save(restaurant1);

        //when, then
        assertThatThrownBy(()->findRestaurantService.findAllRestaurants(Pageable.ofSize(10),
                RestaurantStatus.DELETED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상태의 레스토랑 조회가 불가능합니다.");
    }

}
