package com.orderingsystem.order.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.domain.event.OrderRejectedEvent;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRestaurantApprovalServiceRejectTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private PaymentOutboxHelper paymentOutboxHelper;

    @Mock
    private OrderDataMapper orderDataMapper;

    @InjectMocks
    private OrderRestaurantApprovalService orderRestaurantApprovalService;

    @DisplayName("레스토랑이 주문을 거절하면 주문 거절 처리를 시작한다.")
    @Test
    void shouldSavePaymentOutboxMessage_whenRestaurantRejectsOrder() {
        //given
        UUID messageId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        RestaurantOrderDecisionResponse response = mock(RestaurantOrderDecisionResponse.class);
        given(response.getId()).willReturn(messageId);
        given(response.getOrderId()).willReturn(orderId);
        given(response.getSagaId()).willReturn(sagaId);

        given(processedMessageRepository.insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_REJECT.name()), any(
                ZonedDateTime.class))).willReturn(1);

        Order order = mock(Order.class);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderRejectedEvent orderRejectedEvent = mock(OrderRejectedEvent.class);
        given(order.rejecting()).willReturn(orderRejectedEvent);
        given(orderRejectedEvent.getOrder()).willReturn(order);

        given(order.getOrderStatus()).willReturn(OrderStatus.REJECTING);

        OrderPaymentEventPayload paymentEventPayload = mock(OrderPaymentEventPayload.class);
        given(orderDataMapper.orderRejectedEventToOrderPaymentEventPayload(eq(orderRejectedEvent),
                eq(sagaId))).willReturn(paymentEventPayload);

        //when
        orderRestaurantApprovalService.rejecting(response);

        //then
        verify(paymentOutboxHelper).savePaymentOutboxMessage(eq(paymentEventPayload), eq(OrderStatus.REJECTING), eq(
                SagaStatus.COMPENSATING), eq(sagaId));
        verify(order).rejecting();
    }

    @DisplayName("레스토랑 거절 메시지가 이미 처리되었으면 다시 처리하지 않는다.")
    @Test
    void shouldNotProcessRejection_whenMessageAlreadyHandled() {
        //given
        UUID messageId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        RestaurantOrderDecisionResponse response = mock(RestaurantOrderDecisionResponse.class);
        given(response.getId()).willReturn(messageId);
        given(response.getOrderId()).willReturn(orderId);
        given(response.getSagaId()).willReturn(sagaId);

        given(processedMessageRepository.insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_REJECT.name()),
                any(ZonedDateTime.class)))
                .willReturn(0);

        //when
        orderRestaurantApprovalService.rejecting(response);

        //then
        verifyNoInteractions(orderRepository, orderDataMapper, paymentOutboxHelper);
        verify(processedMessageRepository).insertIgnore(eq(messageId), eq(MessageType.RESTAURANT_REJECT.name()),
                any(ZonedDateTime.class));
    }

    @DisplayName("주문을 최종 거절 처리한다.")
    @Test
    void shouldMarkOrderRejected_whenPaymentDeclined() {
        //given
        UUID orderId = UUID.randomUUID();
        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        given(paymentResponse.getOrderId()).willReturn(orderId);

        Order order =mock(Order.class);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        //when
        orderRestaurantApprovalService.reject(paymentResponse);

        //then
        verify(order).reject();
    }

}
