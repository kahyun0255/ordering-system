package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
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
    private RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Mock
    private OrderApprovalService orderApprovalService;

    @InjectMocks
    private OrderApprovalFacade orderApprovalFacade;

    @DisplayName("레스토랑 소유자라면 주문 승인 처리를 위임한다.")
    @Test
    void shouldApproveOrder_whenUserIsRestaurantOwner() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        RestaurantOwnership restaurantOwnership = RestaurantOwnership.builder()
                .id(100L)
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build();

        given(restaurantOwnershipRepository.findByOwnerIdAndRestaurantId(ownerId, restaurantId))
                .willReturn(Optional.of(restaurantOwnership));

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

        given(restaurantOwnershipRepository.findByOwnerIdAndRestaurantId(ownerId, restaurantId))
                .willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderApprovalFacade.approve(orderId, restaurantId, ownerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 레스토랑의 주문을 승인할 권한이 없습니다.");
    }

}
