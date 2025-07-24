package com.orderingsystem.order.application;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantApprovalOutboxHelper;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.PaymentOutboxRepository;
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
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final RestaurantApprovalOutboxHelper restaurantApprovalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(PaymentResponse paymentResponse) {
        log.info("해당 주문의 결제 처리를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Optional<PaymentOutbox> paymentOutboxMessageResponse = paymentOutboxRepository.getPaymentOutboxBySagaIdAndSagaStatus(
                paymentResponse.getSagaId(),
                SagaStatus.STARTED);

        if (paymentOutboxMessageResponse.isEmpty()){
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 처리 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    paymentResponse.getSagaId());
            return;
        }
        PaymentOutbox paymentOutboxMessage = paymentOutboxMessageResponse.get();

        OrderPaidEvent orderPaidEvent = completedPaymentForOrder(paymentResponse);

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderPaidEvent.getOrder().getOrderStatus());

        paymentOutboxHelper.save(updatePaymentOutboxMessage(paymentOutboxMessage, orderPaidEvent.getOrder().getOrderStatus(), sagaStatus));

        restaurantApprovalOutboxHelper.saveRestaurantApprovalOutboxMessage(
                orderDataMapper.orderPaidEventToRestaurantApprovalEventPayload(orderPaidEvent),
                orderPaidEvent.getOrder().getOrderStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                paymentResponse.getSagaId()
        );

        log.info("해당 주문의 결제 처리가 성공적으로 완료되었습니다. Order Id : {} ", paymentResponse.getOrderId());
    }

    private OrderPaidEvent completedPaymentForOrder(PaymentResponse paymentResponse) {
        log.info("결제 처리 시작. Order Id : {}", paymentResponse.getOrderId());

        Order order = findOrder(paymentResponse.getOrderId());
        return payOrderService.payOrder(order);
    }

    private PaymentOutbox updatePaymentOutboxMessage(PaymentOutbox paymentOutboxMessage, OrderStatus orderStatus,
                                                     SagaStatus sagaStatus) {
        paymentOutboxMessage.updateProcessedAt(ZonedDateTime.now());
        paymentOutboxMessage.updateOrderStatus(orderStatus);
        paymentOutboxMessage.updateSagaStatus(sagaStatus);
        return paymentOutboxMessage;
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse paymentResponse) {
        log.info("해당 주문의 주문 취소를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Order order = findOrder(paymentResponse.getOrderId());
        order.cancel(paymentResponse.getFailureMessages());
        orderRepository.save(order);

        log.info("해당 주문의 주문 취소가 성공적으로 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());
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
