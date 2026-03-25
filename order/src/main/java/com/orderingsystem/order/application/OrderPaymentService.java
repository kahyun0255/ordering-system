package com.orderingsystem.order.application;

import static com.orderingsystem.common.domain.status.OrderStatus.CANCELLING;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.domain.status.PaymentStatus;
import com.orderingsystem.common.saga.SagaConstants;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.coupon.CouponOutboxHelper;
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
    private final CouponOutboxHelper couponOutboxHelper;

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

        boolean hasCoupon = order.hasCoupon();

        OrderPaidEvent orderPaidEvent = order.pay();

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderPaidEvent.getOrder().getOrderStatus());

        if (hasCoupon) {
            if (couponOutboxHelper.isCouponProcessed(paymentResponse.getSagaId())) {
                log.info("주문 결제 및 쿠폰 완료. OrderId : [{}], SagaId : [{}]",
                        paymentResponse.getOrderId(), paymentResponse.getSagaId());
                sendToRestaurant(paymentResponse.getSagaId(), orderPaidEvent, sagaStatus, paymentOutboxMessage);
            } else {
                log.info("결제는 완료되었으나 쿠폰 처리 대기중. OrderId : [{}], SagaId : [{}]",
                        paymentResponse.getOrderId(), paymentResponse.getSagaId());
                updatePaymentOutboxMessage(paymentOutboxMessage, orderPaidEvent.getOrder().getOrderStatus(),
                        sagaStatus);
                return;
            }
        } else {
            log.info("쿠폰 없는 주문 결제가 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());
            sendToRestaurant(paymentResponse.getSagaId(), orderPaidEvent, sagaStatus, paymentOutboxMessage);
        }
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse paymentResponse) {
        log.info("해당 주문의 주문 취소를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Optional<PaymentOutbox> paymentOutboxMessageResponse = paymentOutboxHelper.getPaymentOutboxBySagaId(paymentResponse.getSagaId());

        if (paymentOutboxMessageResponse.isEmpty()) {
            log.warn("해당 Saga Id : {} 에 대한 Outbox 메시지를 찾을 수 없습니다.", paymentResponse.getSagaId());
            return;
        }

        PaymentOutbox paymentOutbox = paymentOutboxMessageResponse.get();

        if (SagaStatus.COMPENSATED == paymentOutbox.getSagaStatus()) {
            log.info("이미 롤백(COMPENSATED) 처리가 완료된 Saga 입니다. Saga Id: {}", paymentResponse.getSagaId());
            return;
        }

        if (SagaStatus.SUCCEEDED == paymentOutbox.getSagaStatus()) {
            log.warn("이미 처리(SUCCEEDED)된 주문에 대한 비정상 롤백 요청입니다. Order Id: {}", paymentResponse.getOrderId());
        }
        Order order = findOrder(paymentResponse.getOrderId());

        if (checkAndMarkProcessed(paymentResponse, MessageType.PAYMENT_ROLLBACK)) {
            return;
        }

        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());

        if (order.getOrderStatus() != OrderStatus.CANCELLED) {
            rollbackPaymentForOrder(order, paymentResponse);

            log.info("결제 실패/취소로 인한 주문 취소 처리. Order Id : [{}]", order.getId());

            sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());

            if (order.hasCoupon()) {
                couponOutboxHelper.saveCouponOutboxMessage(
                        orderDataMapper.orderToCouponRollbackEventPayload(order, paymentResponse.getSagaId()),
                        order.getOrderStatus(),
                        sagaStatus,
                        paymentResponse.getSagaId()
                );
            }

            productOutboxHelper.saveProductOutboxMessage(
                    orderDataMapper.orderToStockReservationCancelEventPayload(order, paymentResponse.getSagaId(),
                            SagaConstants.INVENTORY_COMPENSATE),
                    SagaConstants.INVENTORY_COMPENSATE,
                    sagaStatus,
                    paymentResponse.getSagaId()
            );
        } else {
            log.info("주문이 이미 취소 상태이므로 추가적인 보상 트랜잭션 발행하지 않음. Order Id : [{}]", order.getId());
        }
        updatePaymentOutboxMessage(paymentOutbox, order.getOrderStatus(), sagaStatus);

        log.info("해당 주문의 주문 취소가 성공적으로 완료되었습니다. Order Id : {}", paymentResponse.getOrderId());
    }

    private boolean checkAndMarkProcessed(PaymentResponse paymentRequest, MessageType messageType) {
        int inserted = processedMessageRepository.insertIgnore(
                paymentRequest.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 {} 메시지가 처리되었습니다. Order Id : {}, Saga Id : {}",
                    messageType, paymentRequest.getOrderId(), paymentRequest.getSagaId());
            return true;
        }
        return false;
    }

    private SagaStatus[] getCurrentSagaStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> new SagaStatus[]{SagaStatus.STARTED};
            case CANCELLED, REFUNDED -> new SagaStatus[]{SagaStatus.SUCCEEDED};
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

    private void sendToRestaurant(UUID sagaId, OrderPaidEvent orderPaidEvent, SagaStatus sagaStatus,
                                  PaymentOutbox paymentOutboxMessage) {
        updatePaymentOutboxMessage(paymentOutboxMessage, orderPaidEvent.getOrder().getOrderStatus(), sagaStatus);

        restaurantAcceptOutboxHelper.saveRestaurantAcceptOutboxMessage(
                orderDataMapper.orderPaidEventToRestaurantAcceptEventPayload(orderPaidEvent, sagaId),
                orderPaidEvent.getOrder().getOrderStatus(),
                sagaStatus,
                sagaId
        );
    }

}
