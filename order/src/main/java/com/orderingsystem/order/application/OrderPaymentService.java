package com.orderingsystem.order.application;

import static com.orderingsystem.common.domain.status.OrderStatus.CANCELLING;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaConstants;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.product.ProductOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantAcceptOutboxHelper;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
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
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final RestaurantAcceptOutboxHelper restaurantAcceptOutboxHelper;
    private final OrderDataMapper orderDataMapper;
    private final ProcessedMessageRepository processedMessageRepository;
    private final ProductOutboxHelper productOutboxHelper;

    @Override
    @Transactional
    public void process(PaymentResponse paymentResponse) {
        log.info("해당 주문의 결제 처리를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Optional<PaymentOutbox> paymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxBySagaIdAndSagaStatus(
                paymentResponse.getSagaId(),
                SagaStatus.STARTED);

        if (paymentOutboxMessageResponse.isEmpty()) {
            log.info("STARTED 상태의 PaymentOutbox 없음. SagaId: {}", paymentResponse.getSagaId());
            return;
        }

        PaymentOutbox paymentOutboxMessage = paymentOutboxMessageResponse.get();

        Order order = findOrder(paymentResponse.getOrderId());

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == CANCELLING) {
            log.info("주문이 이미 승인/취소 처리된 상태입니다. 결제 처리를 생략합니다. Order Id : {}", paymentResponse.getOrderId());
            return;
        }

        if (checkAndMarkProcessed(paymentResponse, MessageType.PAYMENT_COMPLETE)) {
            return;
        }

        OrderPaidEvent orderPaidEvent = order.pay();
        log.info("주문 결제가 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderPaidEvent.getOrder().getOrderStatus());

        updatePaymentOutboxMessage(paymentOutboxMessage, orderPaidEvent.getOrder().getOrderStatus(), sagaStatus);

        restaurantAcceptOutboxHelper.saveRestaurantAcceptOutboxMessage(
                orderDataMapper.orderPaidEventToRestaurantAcceptEventPayload(orderPaidEvent,
                        paymentResponse.getSagaId()),
                orderPaidEvent.getOrder().getOrderStatus(),
                sagaStatus,
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

        if (checkAndMarkProcessed(paymentResponse, MessageType.PAYMENT_ROLLBACK)) {
            return;
        }

        rollbackPaymentForOrder(order, paymentResponse);

        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());
        updatePaymentOutboxMessage(paymentOutbox, order.getOrderStatus(), sagaStatus);

        productOutboxHelper.saveProductOutboxMessage(
                orderDataMapper.orderToStockReservationCancelEventPayload(order, paymentResponse.getSagaId(), SagaConstants.INVENTORY_COMPENSATE),
                SagaConstants.INVENTORY_COMPENSATE,
                sagaStatus,
                paymentResponse.getSagaId()
        );

        log.info("해당 주문의 주문 취소가 성공적으로 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());
    }

    private boolean checkAndMarkProcessed(PaymentResponse paymentRequest, MessageType messageType) {
        int inserted = processedMessageRepository.insertIgnore(
                paymentRequest.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );

        log.info("이미 {} 메시지가 처리되었습니다. Order Id : {}, Saga Id : {}", messageType, paymentRequest.getOrderId(),
                paymentRequest.getSagaId());

        return inserted == 0;
    }

    private SagaStatus[] getCurrentSagaStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> new SagaStatus[]{SagaStatus.STARTED};
            case CANCELLED -> new SagaStatus[]{SagaStatus.PROCESSING, SagaStatus.COMPENSATING};
            case FAILED -> new SagaStatus[]{SagaStatus.STARTED, SagaStatus.PROCESSING};
        };
    }

    private void rollbackPaymentForOrder(Order order, PaymentResponse paymentResponse) {
        order.cancel(paymentResponse.getFailureMessages());
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
