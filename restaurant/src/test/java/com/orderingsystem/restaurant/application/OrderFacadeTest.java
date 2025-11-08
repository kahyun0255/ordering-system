package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private RestaurantAccessValidatorService restaurantAccessValidatorService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderFacade orderFacade;

    @DisplayName("레스토랑의 소유자이고, 영업 중, 영업 전, 일시 휴업 상태라면 주문 조회를 위임한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideValidRestaurantStatusesForOrderAccess")
    void shouldQueryOrders_whenOwnerAndStatusIsAccessible(String rs, RestaurantStatus restaurantStatus) {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        PageRequest pageRequest = PageRequest.of(0, 10);
        String status = OrderApprovalStatus.APPROVED.name();

        Restaurant restaurant = Restaurant.builder()
                .name("레스토랑")
                .restaurantId(restaurantId)
                .status(restaurantStatus)
                .build();

        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);
        given(restaurantAccessValidatorService.findRestaurant(restaurantId)).willReturn(restaurant);

        //when
        orderFacade.getOrders(restaurantId, ownerId, pageRequest, status);

        //then
        verify(orderService).findOrders(restaurantId, pageRequest, status);
        verify(restaurantAccessValidatorService).isRestaurantOwnership(ownerId, restaurantId);
        verify(restaurantAccessValidatorService).findRestaurant(restaurantId);
    }

    private static Stream<Arguments> provideValidRestaurantStatusesForOrderAccess() {
        return Stream.of(
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("관리자 승인 대기, 폐업, 영업 정지 상태의 레스토랑일 경우 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideInvalidRestaurantStatusesForOrderAccess")
    void shouldReturnBadRequest_whenRestaurantStatusIsNotAccessible(String rs, RestaurantStatus restaurantStatus) {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        PageRequest pageRequest = PageRequest.of(0, 10);
        String status = OrderApprovalStatus.APPROVED.name();

        Restaurant restaurant = Restaurant.builder()
                .name("레스토랑")
                .restaurantId(restaurantId)
                .status(restaurantStatus)
                .build();

        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);
        given(restaurantAccessValidatorService.findRestaurant(restaurantId)).willReturn(restaurant);

        //when, then
        assertThatThrownBy(() -> orderFacade.getOrders(restaurantId, ownerId, pageRequest, status))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("레스토랑이 주문 정보를 조회할 수 없는 상태입니다.");
    }

    private static Stream<Arguments> provideInvalidRestaurantStatusesForOrderAccess() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑의 주문 정보를 확인할 권한이 없으면 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserHasNoPermissionToViewOrders() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        PageRequest pageRequest = PageRequest.of(0, 10);
        String status = OrderApprovalStatus.APPROVED.name();

        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> orderFacade.getOrders(restaurantId, ownerId, pageRequest, status))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("주문 정보를 확인할 권한이 없습니다.");
    }

}
