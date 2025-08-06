package com.orderingsystem.order.application;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantApprovalOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.PayOrderService;
import com.orderingsystem.outbox.OutboxStatus;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService implements SagaStep<PaymentResponse> {

    private final OrderRepository orderRepository;
    private final PayOrderService payOrderService;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(PaymentResponse paymentResponse) {
        log.info("해당 주문의 결제 처리를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Optional<PaymentOutbox> paymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxBySagaIdAndSagaStatus(
                paymentResponse.getSagaId(),
                SagaStatus.STARTED);

        if (paymentOutboxMessageResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 처리 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    paymentResponse.getSagaId());
            return;
        }
        PaymentOutbox paymentOutboxMessage = paymentOutboxMessageResponse.get();

        Order order = findOrder(paymentResponse.getOrderId());

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == OrderStatus.CANCELLING) {
            log.info("주문이 이미 승인/취소 처리된 상태입니다. 결제 처리를 생략합니다. Order Id : {}", paymentResponse.getOrderId());
            return;
        }

        OrderPaidEvent orderPaidEvent = completedPaymentForOrder(order);

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderPaidEvent.getOrder().getOrderStatus());

        updatePaymentOutboxMessage(paymentOutboxMessage, orderPaidEvent.getOrder().getOrderStatus(), sagaStatus);

        restaurantApprovalOutboxHelper.saveRestaurantApprovalOutboxMessage(
                orderDataMapper.orderPaidEventToRestaurantApprovalEventPayload(orderPaidEvent,
                        paymentResponse.getSagaId()),
                orderPaidEvent.getOrder().getOrderStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                paymentResponse.getSagaId()
        );

        log.info("해당 주문의 결제 처리가 성공적으로 완료되었습니다. Order Id : {} ", paymentResponse.getOrderId());
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse paymentResponse) {
        log.info("해당 주문의 주문 취소를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Optional<PaymentOutbox> paymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxBySagaIdAndSagaStatus(
                paymentResponse.getSagaId(),
                getCurrentSagaStatus(PaymentStatus.valueOf(paymentResponse.getPaymentStatus())));

        if (paymentOutboxMessageResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 롤백되어 메시지를 다시 처리하지 않습니다.",
                    paymentResponse.getSagaId());
            return;
        }
        PaymentOutbox paymentOutbox = paymentOutboxMessageResponse.get();

        Order order = findOrder(paymentResponse.getOrderId());

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.info("주문이 이미 승인/취소 처리된 상태입니다. 결제 처리를 생략합니다. Order Id : {}", paymentResponse.getOrderId());
            return;
        }

        rollbackPaymentForOrder(order, paymentResponse);

        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());
        updatePaymentOutboxMessage(paymentOutbox, order.getOrderStatus(), sagaStatus);

        if (paymentResponse.getPaymentStatus().equals(PaymentStatus.CANCELLED.name())) {
            updateApprovalOutboxMessage(paymentResponse.getSagaId(), order.getOrderStatus(), sagaStatus);
        }

        log.info("해당 주문의 주문 취소가 성공적으로 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());
    }

    private OrderPaidEvent completedPaymentForOrder(Order order) {
        log.info("결제 처리 시작. Order Id : {}", order.getId());
        return payOrderService.payOrder(order);
    }

    private SagaStatus[] getCurrentSagaStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> new SagaStatus[]{SagaStatus.STARTED};
            case CANCELLED -> new SagaStatus[]{SagaStatus.PROCESSING};
            case FAILED -> new SagaStatus[]{SagaStatus.STARTED, SagaStatus.PROCESSING};
        };
    }

    private void rollbackPaymentForOrder(Order order, PaymentResponse paymentResponse) {
        order.cancel(paymentResponse.getFailureMessages());
    }

    private void updateApprovalOutboxMessage(UUID sagaId, OrderStatus orderStatus,
                                             SagaStatus sagaStatus) {
        Optional<RestaurantApprovalOutbox> restaurantApprovalOutboxResponse =
                restaurantApprovalOutboxHelper.getRestaurantApprovalOutboxBySagaIdAndSagaStatus(sagaId,
                        SagaStatus.COMPENSATING);

        if (restaurantApprovalOutboxResponse.isEmpty()) {
            throw new OrderApplicationException(
                    "SagaStatus " + SagaStatus.COMPENSATING.name() + " 상태의 RestaurantApprovalOutbox를 찾지 못했습니다.");
        }

        RestaurantApprovalOutbox restaurantApprovalOutbox = restaurantApprovalOutboxResponse.get();
        restaurantApprovalOutbox.updateProcessedAt(ZonedDateTime.now());
        restaurantApprovalOutbox.updateOrderStatus(orderStatus);
        restaurantApprovalOutbox.updateSagaStatus(sagaStatus);
    }

    private void updatePaymentOutboxMessage(PaymentOutbox paymentOutboxMessage, OrderStatus orderStatus,
                                            SagaStatus sagaStatus) {
        paymentOutboxMessage.updateProcessedAt(ZonedDateTime.now());
        paymentOutboxMessage.updateOrderStatus(orderStatus);
        paymentOutboxMessage.updateSagaStatus(sagaStatus);
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문을 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문을 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }
}
