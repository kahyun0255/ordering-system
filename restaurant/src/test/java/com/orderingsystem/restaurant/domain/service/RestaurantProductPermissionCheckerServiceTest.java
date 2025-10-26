package com.orderingsystem.restaurant.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RestaurantProductPermissionCheckerServiceTest {

    private RestaurantProductPermissionCheckerService restaurantProductPermissionCheckerService;

    @BeforeEach
    void setUp() {
        restaurantProductPermissionCheckerService = new RestaurantProductPermissionCheckerService();
    }

    @DisplayName("관리자 승인 대기, 영업 전, 영업 중, 일시 휴업 상태의 레스토랑은 true를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideProductManageableStatuses")
    void shouldAllowProductAddition_whenRestaurantStatusIsValid(String status, RestaurantStatus restaurantStatus) {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(restaurantStatus)
                .name("레스토랑")
                .build();

        //when, then
        assertThat(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).isTrue();
    }

    private static Stream<Arguments> provideProductManageableStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("폐업, 영업 정지, 삭제 상태의 레스토랑은 false를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("restaurantStatusesThatCannotExposeProducts")
    void tes1t(String status, RestaurantStatus restaurantStatus) {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(restaurantStatus)
                .name("레스토랑")
                .build();

        //when, then
        assertThat(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).isFalse();
    }

    private static Stream<Arguments> restaurantStatusesThatCannotExposeProducts() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED),
                Arguments.of("삭제", RestaurantStatus.DELETED)
        );
    }

}
