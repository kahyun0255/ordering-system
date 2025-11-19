package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.restaurant.application.outbox.order.model.OrderEventPayload;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceCancelTest {

    @Mock
    private OrderApprovalRepository orderApprovalRepository;

    @Mock
    private RestaurantDataMapper restaurantDataMapper;

    @Mock
    private OrderOutboxHelper orderOutboxHelper;

    @InjectMocks
    private ProductStockService productStockService;

    @DisplayName("주문을 취소할 때, 주문 내역이 존재하면 주문 취소에 성공한다.")
    @Test
    void shouldCancelOrder_whenOrderExists() {
        //given
        Map<Object, Object> confirmed = new HashMap<>();
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .restaurantId(UUID.randomUUID())
                .status(OrderApprovalStatus.ACCEPTED)
                .build();

        OrderEventPayload payload = OrderEventPayload.builder().build();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.of(orderApproval));
        given(restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(any())).willReturn(payload);

        //when
        productStockService.cancel(confirmed, orderId, sagaId);

        //then
        verify(orderOutboxHelper).saveOrderOutboxMessage(payload, OrderApprovalStatus.CANCELLED, sagaId);
        assertThat(orderApproval.getStatus()).isEqualTo(OrderApprovalStatus.CANCELLED);
    }

    @DisplayName("주문을 취소할 때, 주문 내역이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderDoesNotExist() {
        //given
        Map<Object, Object> confirmed = new HashMap<>();
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> productStockService.cancel(confirmed, orderId, sagaId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("주문 정보를 찾을 수 없습니다.");
    }

    @DisplayName("예약을 취소할 때, 주문 내역이 존재하면 주문이 취소된다.")
    @Test
    void shouldCancelReservation_whenOrderExists() {
        //given
        UUID orderId = UUID.randomUUID();

        OrderApproval orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .restaurantId(UUID.randomUUID())
                .status(OrderApprovalStatus.ACCEPTED)
                .build();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.of(orderApproval));

        //when
        productStockService.cancelReservation(orderId);

        //then
        assertThat(orderApproval.getStatus()).isEqualTo(OrderApprovalStatus.CANCELLED);
    }

    @DisplayName("예약을 취소할 때, 주문 내역이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenCancellingReservationAndOrderDoesNotExist() {
        //given
        UUID orderId = UUID.randomUUID();

        given(orderApprovalRepository.findByOrderId(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> productStockService.cancelReservation(orderId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("주문 정보를 찾을 수 없습니다.");
    }

}
