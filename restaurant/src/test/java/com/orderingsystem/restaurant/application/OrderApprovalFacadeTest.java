package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantStatusValidatorService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApprovalFacadeTest {

    @Mock
    private OrderApprovalService orderApprovalService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantStatusValidatorService restaurantStatusValidatorService;

    @Mock
    private RestaurantAccessValidatorService restaurantAccessValidatorService;

    @InjectMocks
    private OrderApprovalFacade orderApprovalFacade;

    @DisplayName("레스토랑 소유자라면 주문 승인 처리를 위임한다.")
    @Test
    void shouldApproveOrder_whenUserIsRestaurantOwner() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);

        //when
        orderApprovalFacade.approve(orderId, restaurantId, ownerId);

        //then
        verify(orderApprovalService, times(1)).approval(restaurantId, orderId, ownerId);
    }

    @DisplayName("레스토랑 소유자가 아니면 주문 승인 시도시 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserIsNotRestaurantOwner() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> orderApprovalFacade.approve(orderId, restaurantId, ownerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 레스토랑의 주문을 승인할 권한이 없습니다.");
    }

    @DisplayName("레스토랑이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantDoesNotExist() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        //when
        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        //then
        assertThatThrownBy(() -> orderApprovalFacade.approve(orderId, restaurantId, ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

}
