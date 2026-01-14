package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.CouponResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.coupon.CouponOutboxHelper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantAcceptOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantAcceptEventPayload;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.CouponOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCouponServiceTest {

    @InjectMocks
    private OrderCouponService orderCouponService;

    @Mock
    private CouponOutboxHelper couponOutboxHelper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private RestaurantAcceptOutboxHelper restaurantAcceptOutboxHelper;

    @Mock
    private OrderDataMapper orderDataMapper;

    @Mock
    private PaymentOutboxHelper paymentOutboxHelper;

    @Test
    @DisplayName("결제가 이미 완료(PAID)된 상태라면, 쿠폰 처리를 완료하고 레스토랑 승인 요청을 보낸다.")
    void shouldCompleteCouponAndRequestRestaurantApproval_whenPaymentIsAlreadyPaid() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId, SagaStatus.STARTED);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.PAID);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(1);

        RestaurantAcceptEventPayload payload = RestaurantAcceptEventPayload.builder().build();
        given(orderDataMapper.orderToRestaurantAcceptEventPayload(order, sagaId)).willReturn(payload);

        //when
        orderCouponService.process(couponResponse);

        //then
        verify(couponOutbox).updateSagaStatus(SagaStatus.SUCCEEDED);
        verify(couponOutbox).updateOrderStatus(OrderStatus.PAID);

        verify(restaurantAcceptOutboxHelper).saveRestaurantAcceptOutboxMessage(
                eq(payload),
                eq(OrderStatus.PAID),
                any(SagaStatus.class),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("결제가 아직 대기중(PENDING)이라면, 쿠폰 처리만 완료하고 레스토랑 요청은 보내지 않는다.")
    void shouldCompleteCouponWithoutRequestingRestaurantApproval_whenPaymentIsPending() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId, SagaStatus.STARTED);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.PENDING);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(1);

        //when
        orderCouponService.process(couponResponse);

        //then
        verify(couponOutbox).updateSagaStatus(SagaStatus.SUCCEEDED);
        verify(restaurantAcceptOutboxHelper, never()).saveRestaurantAcceptOutboxMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Saga Outbox 데이터가 없다면 아무것도 하지 않는다.")
    void shouldDoNothing_whenNoOutboxMessageExists() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.empty());

        //when
        orderCouponService.process(couponResponse);

        //then
        verify(orderRepository, never()).findById(any());
        verify(restaurantAcceptOutboxHelper, never()).saveRestaurantAcceptOutboxMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문이 이미 취소(CANCELLED)된 상태라면 처리를 중단한다.")
    void shouldStopProcessing_whenOrderIsAlreadyCancelled() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId, SagaStatus.STARTED);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.CANCELLED);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        //when
        orderCouponService.process(couponResponse);

        //then
        verify(processedMessageRepository, never()).insertIgnore(any(), any(), any());
        verify(restaurantAcceptOutboxHelper, never()).saveRestaurantAcceptOutboxMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 처리된 메시지라면 처리를 중단한다.")
    void shouldIgnoreProcessing_whenMessageAlreadyHandled() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId, SagaStatus.STARTED);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.PAID);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(0);

        //when
        orderCouponService.process(couponResponse);

        //then
        verify(couponOutbox, never()).updateSagaStatus(any());
        verify(restaurantAcceptOutboxHelper, never()).saveRestaurantAcceptOutboxMessage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("주문을 찾을 수 없으면 예외가 발생한다.")
    void shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        CouponResponse couponResponse = createCouponResponse(orderId, sagaId);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId, SagaStatus.STARTED);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.STARTED))
                .willReturn(Optional.of(couponOutbox));

        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> orderCouponService.process(couponResponse))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @DisplayName("failureMessage가 존재하고, updatedCount가 0 이하면 쿠폰 사용에 실패했기 때문에 outbox FAILED로 업데이트하고, 주문을 CANCELLING 상태로 바꾸며 결제 취소 요청을 보낸다.")
    @Test
    void shouldRollbackAndTriggerCompensation_whenCouponFailed() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        List<String> failureMessages = List.of("쿠폰 만료", "재고 부족");

        CouponResponse couponResponse = createCouponResponse(orderId, sagaId, failureMessages);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(eq(sagaId), eq(SagaStatus.PROCESSING)))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.PENDING);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(1);

        given(orderDataMapper.orderToPaymentRollbackEventPayload(any(), any())).willReturn(
                mock(OrderPaymentEventPayload.class));

        //when
        orderCouponService.rollback(couponResponse);

        //then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLING);
        assertThat(order.getFailureMessageList()).containsAll(failureMessages);
        verify(couponOutbox).updateSagaStatus(SagaStatus.FAILED);

        verify(paymentOutboxHelper).savePaymentOutboxMessage(any(OrderPaymentEventPayload.class),
                eq(OrderStatus.CANCELLING), eq(SagaStatus.FAILED), eq(sagaId));
    }

    @DisplayName("주문이 이미 취소된(CANCELLED) 경우, outbox만 업데이트하고 추가 보상 트랜잭션은 생략한다.")
    @Test
    void shouldUpdateOutboxButSkipCompensation_whenOrderAlreadyCancelled() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        List<String> failureMessages = List.of("Error");

        CouponResponse couponResponse = createCouponResponse(orderId, sagaId, failureMessages);

        CouponOutbox couponOutbox = createCouponOutbox(sagaId);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(eq(sagaId), eq(SagaStatus.PROCESSING)))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.CANCELLED);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(1);

        //when
        orderCouponService.rollback(couponResponse);

        //then
        verify(couponOutbox).updateSagaStatus(SagaStatus.FAILED);
        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    @DisplayName("롤백시 처리 할 outbox 메시지가 없으면(이미 처리됨 등) 아무것도 하지 않는다.")
    @Test
    void shouldDoNothingOnRollback_whenNoOutboxFound() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        CouponResponse couponResponse = createCouponResponse(orderId, sagaId, List.of("Error"));

        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(eq(sagaId), eq(SagaStatus.PROCESSING)))
                .willReturn(Optional.empty());

        //when
        orderCouponService.rollback(couponResponse);

        //then
        verify(orderRepository, never()).findById(any());
        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    @DisplayName("롤백시 이미 처리된 메시지(멱등성 체크)라면 중복해서 처리하지 않는다.")
    @Test
    void shouldIgnoreRollback_whenMessageAlreadyHandled() {
        //given
        UUID orderId = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();

        CouponResponse couponResponse = createCouponResponse(orderId, sagaId, List.of("Error"));

        CouponOutbox couponOutbox = createCouponOutbox(sagaId);
        given(couponOutboxHelper.getCouponOutboxBySagaIdAndSagaStatus(eq(sagaId), eq(SagaStatus.PROCESSING)))
                .willReturn(Optional.of(couponOutbox));

        Order order = createOrder(orderId, OrderStatus.PAID);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        given(processedMessageRepository.insertIgnore(any(), any(), any())).willReturn(0);

        //when
        orderCouponService.rollback(couponResponse);

        //then
        verify(couponOutbox, never()).updateSagaStatus(any());
        verify(paymentOutboxHelper, never()).savePaymentOutboxMessage(any(), any(), any(), any());
    }

    private CouponResponse createCouponResponse(UUID orderId, UUID sagaId) {
        return createCouponResponse(orderId, sagaId, null);
    }

    private CouponResponse createCouponResponse(UUID orderId, UUID sagaId, List<String> failureMessages) {
        return CouponResponse.builder()
                .orderId(orderId)
                .sagaId(sagaId)
                .id(UUID.randomUUID())
                .failureMessages(failureMessages)
                .build();
    }

    private Order createOrder(UUID orderId, OrderStatus status) {
        return Order.builder()
                .id(orderId)
                .orderStatus(status)
                .build();
    }

    private CouponOutbox createCouponOutbox(UUID sagaId) {
        return mock(CouponOutbox.class);
    }

    private CouponOutbox createCouponOutbox(UUID sagaId, SagaStatus sagaStatus) {
        return mock(CouponOutbox.class);
    }

}
