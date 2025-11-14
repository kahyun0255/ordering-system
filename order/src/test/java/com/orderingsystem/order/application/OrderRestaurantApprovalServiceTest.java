package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRestaurantApprovalServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @InjectMocks
    private OrderRestaurantApprovalService orderRestaurantApprovalService;

    @DisplayName("처리되지 않은 메시지라면 주문을 승인한다.")
    @Test
    void shouldApproveOrder_whenMessageIsNotAlreadyProcessed() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        RestaurantOrderDecisionResponse response = RestaurantOrderDecisionResponse.builder()
                .id(messageId)
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .createdAt(Instant.now())
                .failureMessages(new ArrayList<>())
                .build();

        Order order = mock(Order.class);

        given(processedMessageRepository.insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_APPROVAL.name()), any()))
                .willReturn(1);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        //when
        orderRestaurantApprovalService.approve(response);

        //then
        verify(orderRepository).findById(orderId);
        verify(order).approve();
    }

    @DisplayName("이미 처리된 메시지라면 주문 승인 로직을 실행하지 않는다.")
    @Test
    void shouldSkipApproval_whenMessageAlreadyProcessed() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        RestaurantOrderDecisionResponse response = RestaurantOrderDecisionResponse.builder()
                .id(messageId)
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .createdAt(Instant.now())
                .failureMessages(new ArrayList<>())
                .build();

        given(processedMessageRepository.insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_APPROVAL.name()), any()))
                .willReturn(0);

        //when
        orderRestaurantApprovalService.approve(response);

        //then
        verify(orderRepository, never()).findById(any());
    }

    @DisplayName("주문 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenOrderDoesNotExist() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        RestaurantOrderDecisionResponse response = RestaurantOrderDecisionResponse.builder()
                .id(messageId)
                .sagaId(sagaId)
                .orderId(orderId)
                .restaurantId(restaurantId)
                .orderApprovalStatus(OrderApprovalStatus.APPROVED)
                .createdAt(Instant.now())
                .failureMessages(new ArrayList<>())
                .build();

        given(processedMessageRepository.insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_APPROVAL.name()), any()))
                .willReturn(1);

        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderRestaurantApprovalService.approve(response))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문 정보를 찾을 수 없습니다.");
    }

}
