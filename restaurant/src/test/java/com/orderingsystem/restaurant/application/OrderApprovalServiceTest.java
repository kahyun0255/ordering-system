package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovedEvent;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderRejectedEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderApprovalServiceTest {

    @Mock
    private OrderApprovalRepository orderApprovalRepository;

    @Mock
    private OrderOutboxHelper orderOutboxHelper;

    @Mock
    private RestaurantDataMapper restaurantDataMapper;

    @InjectMocks
    private OrderApprovalService orderApprovalService;

    @DisplayName("주문이 존재하면 주문이 승인된다.")
    @Test
    void shouldApproveOrder_whenOrderExists() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        OrderApproval orderApproval = mock(OrderApproval.class);
        OrderApprovedEvent orderApprovedEvent = mock(OrderApprovedEvent.class);

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.of(orderApproval));
        given(orderApproval.approval()).willReturn(orderApprovedEvent);
        given(orderApprovedEvent.getOrderApproval()).willReturn(orderApproval);
        given(orderApproval.getStatus()).willReturn(OrderApprovalStatus.APPROVED);

        //when
        orderApprovalService.approval(restaurantId, orderId, ownerId);

        //then
        verify(orderApprovalRepository).findByOrderId(orderId);
        verify(orderApproval).approval();
        verify(orderOutboxHelper).saveOrderOutboxMessage(any(), eq(OrderApprovalStatus.APPROVED), any(UUID.class));
    }

    @DisplayName("주문 승인시 주문이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowRestaurantNotFoundException_whenOrderDoesNotExist() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderApprovalService.approval(restaurantId, orderId, ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("주문 내역을 찾을 수 없습니다.");
    }

    @DisplayName("주문이 존재하면 주문이 거절된다.")
    @Test
    void shouldRejectOrder_whenOrderExists() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        OrderApproval orderApproval = mock(OrderApproval.class);
        OrderRejectedEvent orderRejectedEvent = mock(OrderRejectedEvent.class);

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.of(orderApproval));
        given(orderApproval.reject()).willReturn(orderRejectedEvent);
        given(orderRejectedEvent.getOrderApproval()).willReturn(orderApproval);
        given(orderApproval.getStatus()).willReturn(OrderApprovalStatus.REJECTED);

        //when
        orderApprovalService.reject(restaurantId, orderId, ownerId);

        //then
        verify(orderApprovalRepository).findByOrderId(orderId);
        verify(orderApproval).reject();
        verify(orderOutboxHelper).saveOrderOutboxMessage(any(), eq(OrderApprovalStatus.REJECTED), any(UUID.class));
    }

    @DisplayName("주문 거절시 주문이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderApprovalService.reject(restaurantId, orderId, ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("주문 내역을 찾을 수 없습니다.");
    }

}
