package com.orderingsystem.order.application;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.OrderPaymentCancelService;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRestaurantApprovalService implements SagaStep<RestaurantApprovalResponse> {

    private final OrderRepository orderRepository;
    private final OrderPaymentCancelService orderPaymentCancelService;
    private final RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("주문 승인 처리 중. Order Id : {}", restaurantApprovalResponse.getOrderId());

        Optional<RestaurantApprovalOutbox> restaurantApprovalOutboxResponse =
                restaurantApprovalOutboxHelper.getRestaurantApprovalOutboxBySagaIdAndSagaStatus(
                        restaurantApprovalResponse.getSagaId(),
                        SagaStatus.PROCESSING);

        if (restaurantApprovalOutboxResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 처리 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    restaurantApprovalResponse.getSagaId());
            return;
        }

        RestaurantApprovalOutbox restaurantApprovalOutbox = restaurantApprovalOutboxResponse.get();

        Order order = approvalOrder(restaurantApprovalResponse);
        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());

        updateApprovalOutbox(restaurantApprovalOutbox, order.getOrderStatus(), sagaStatus);
        updatePaymentOutbox(restaurantApprovalResponse.getSagaId(), order.getOrderStatus(), sagaStatus);

        log.info("주문이 승인되었습니다. Order Id : {}", restaurantApprovalResponse.getOrderId());
    }

    @Override
    @Transactional
    public void rollback(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("주문 취소 처리 중. Order Id : {}", restaurantApprovalResponse.getOrderId());

        Optional<RestaurantApprovalOutbox> restaurantApprovalOutboxResponse =
                restaurantApprovalOutboxHelper.getRestaurantApprovalOutboxBySagaIdAndSagaStatus(
                        restaurantApprovalResponse.getSagaId(),
                        SagaStatus.PROCESSING);

        if (restaurantApprovalOutboxResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 롤백되어 다시 처리하지 않습니다.",
                    restaurantApprovalResponse.getSagaId());
            return;
        }

        RestaurantApprovalOutbox restaurantApprovalOutbox = restaurantApprovalOutboxResponse.get();

        OrderCancelledEvent orderCancelledEvent = rollbackOrder(restaurantApprovalResponse);

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCancelledEvent.getOrder().getOrderStatus());

        updateApprovalOutbox(restaurantApprovalOutbox, orderCancelledEvent.getOrder().getOrderStatus(), sagaStatus);

        paymentOutboxHelper.savePaymentOutboxMessage(
                orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(orderCancelledEvent),
                orderCancelledEvent.getOrder().getOrderStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                restaurantApprovalResponse.getSagaId());

        log.info("레스토랑 승인 거절로 인해 주문 ID: {} 의 주문을 취소 처리했습니다. failureMessages : {}",
                restaurantApprovalResponse.getOrderId(),
                String.join(",", restaurantApprovalResponse.getFailureMessages()));
    }

    private Order approvalOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
        Order order = findOrder(restaurantApprovalResponse.getOrderId());
        order.approve();
        log.info("주문 승인 완료. Order Id : {}", restaurantApprovalResponse.getOrderId());
        return order;
    }

    private OrderCancelledEvent rollbackOrder(RestaurantApprovalResponse restaurantApprovalResponse) {
        Order order = findOrder(restaurantApprovalResponse.getOrderId());
        return orderPaymentCancelService.cancelOrderPayment(order, restaurantApprovalResponse.getFailureMessages());
    }

    private void updateApprovalOutbox(RestaurantApprovalOutbox restaurantApprovalOutbox,
                                      OrderStatus orderStatus, SagaStatus sagaStatus) {
        restaurantApprovalOutbox.updateProcessedAt(ZonedDateTime.now());
        restaurantApprovalOutbox.updateOrderStatus(orderStatus);
        restaurantApprovalOutbox.updateSagaStatus(sagaStatus);
    }

    private void updatePaymentOutbox(UUID sagaId, OrderStatus orderStatus, SagaStatus sagaStatus) {
        Optional<PaymentOutbox> paymentOutboxResponse =
                paymentOutboxHelper.getPaymentOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.PROCESSING);

        if (paymentOutboxResponse.isEmpty()) {
            throw new OrderApplicationException(
                    "SagaStatus가 {} 상태인 PaymentOutbox를 찾지 못했습니다." + SagaStatus.PROCESSING.name());
        }

        PaymentOutbox paymentOutbox = paymentOutboxResponse.get();
        paymentOutbox.updateProcessedAt(ZonedDateTime.now());
        paymentOutbox.updateOrderStatus(orderStatus);
        paymentOutbox.updateSagaStatus(sagaStatus);
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문 정보를 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문 정보를 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }
}
