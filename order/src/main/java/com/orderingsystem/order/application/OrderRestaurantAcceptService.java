package com.orderingsystem.order.application;

import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import com.orderingsystem.order.application.exception.OrderApplicationException;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.application.outbox.restaurant.RestaurantAcceptOutboxHelper;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.MessageType;
import com.orderingsystem.order.domain.model.outbox.PaymentOutbox;
import com.orderingsystem.order.domain.model.outbox.RestaurantAcceptOutbox;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.repository.outbox.ProcessedMessageRepository;
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
public class OrderRestaurantAcceptService implements SagaStep<RestaurantOrderDecisionResponse> {

    private final OrderRepository orderRepository;
    private final RestaurantAcceptOutboxHelper restaurantAcceptOutboxHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final OrderDataMapper orderDataMapper;
    private final ProcessedMessageRepository processedMessageRepository;

    @Override
    @Transactional
    public void process(RestaurantOrderDecisionResponse restaurantOrderDecisionResponse) {
        log.info("주문 접수 처리 중. Order Id : {}", restaurantOrderDecisionResponse.getOrderId());

        if (checkAndMarkProcessed(restaurantOrderDecisionResponse, MessageType.RESTAURANT_ACCEPT)) {
            return;
        }

        Optional<RestaurantAcceptOutbox> restaurantAcceptOutboxResponse =
                restaurantAcceptOutboxHelper.getRestaurantAcceptOutboxBySagaIdAndSagaStatus(
                        restaurantOrderDecisionResponse.getSagaId(),
                        SagaStatus.PROCESSING);

        if (restaurantAcceptOutboxResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 없거나, 이미 다른 상태로 처리 중이므로 메시지를 무시합니다.",
                    restaurantOrderDecisionResponse.getSagaId());
            return;
        }

        RestaurantAcceptOutbox restaurantAcceptOutbox = restaurantAcceptOutboxResponse.get();

        Order order = findOrder(restaurantOrderDecisionResponse.getOrderId());

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == OrderStatus.CANCELLING) {
            log.info("주문이 이미 {} 상태입니다. Outbox 업데이트를 생략합니다. Order Id : {}", order.getOrderStatus().name(),
                    restaurantOrderDecisionResponse.getOrderId());

            return;
        }

        acceptOrder(order);

        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(order.getOrderStatus());

        updateAcceptOutbox(restaurantAcceptOutbox, order.getOrderStatus(), sagaStatus);
        updatePaymentOutbox(restaurantOrderDecisionResponse.getSagaId(), order.getOrderStatus(), sagaStatus);

        log.info("주문이 접수되었습니다. Order Id : {}", restaurantOrderDecisionResponse.getOrderId());
    }

    @Override
    @Transactional
    public void rollback(RestaurantOrderDecisionResponse restaurantOrderDecisionResponse) {
        log.info("주문 접수 거절 처리 중. Order Id : {}", restaurantOrderDecisionResponse.getOrderId());

        if (checkAndMarkProcessed(restaurantOrderDecisionResponse, MessageType.RESTAURANT_DECLINE)) {
            return;
        }

        Optional<RestaurantAcceptOutbox> restaurantAcceptOutboxResponse =
                restaurantAcceptOutboxHelper.getRestaurantAcceptOutboxBySagaIdAndSagaStatus(
                        restaurantOrderDecisionResponse.getSagaId(),
                        SagaStatus.PROCESSING);

        if (restaurantAcceptOutboxResponse.isEmpty()) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 롤백되어 다시 처리하지 않습니다.",
                    restaurantOrderDecisionResponse.getSagaId());
            return;
        }

        RestaurantAcceptOutbox restaurantAcceptOutbox = restaurantAcceptOutboxResponse.get();

        Order order = findOrder(restaurantOrderDecisionResponse.getOrderId());

        if (order.getOrderStatus() == OrderStatus.APPROVED
                || order.getOrderStatus() == OrderStatus.CANCELLED
                || order.getOrderStatus() == OrderStatus.CANCELLING) {
            log.info("주문이 이미 {} 상태입니다. Outbox 업데이트를 생략합니다. Order Id : {}", order.getOrderStatus().name(),
                    restaurantOrderDecisionResponse.getOrderId());

            return;
        }

        OrderCancelledEvent orderCancelledEvent = order.initCancel(restaurantOrderDecisionResponse.getFailureMessages());

        SagaStatus sagaStatus =
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCancelledEvent.getOrder().getOrderStatus());

        updateAcceptOutbox(restaurantAcceptOutbox, orderCancelledEvent.getOrder().getOrderStatus(), sagaStatus);

        paymentOutboxHelper.savePaymentOutboxMessage(
                orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(orderCancelledEvent,
                        restaurantOrderDecisionResponse.getSagaId()),
                orderCancelledEvent.getOrder().getOrderStatus(),
                sagaStatus,
                restaurantOrderDecisionResponse.getSagaId());

        log.info("레스토랑 접수 실패로 인해 주문 ID: {} 의 주문을 취소 처리했습니다. failureMessages : {}",
                restaurantOrderDecisionResponse.getOrderId(),
                String.join(",", restaurantOrderDecisionResponse.getFailureMessages()));
    }

    private boolean checkAndMarkProcessed(RestaurantOrderDecisionResponse restaurantOrderDecisionResponse,
                                          MessageType messageType) {
        int inserted = processedMessageRepository.insertIgnore(
                restaurantOrderDecisionResponse.getId(),
                messageType.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 {} 메시지가 처리되었습니다. Order Id : {}, Saga Id : {}",
                    messageType,
                    restaurantOrderDecisionResponse.getOrderId(),
                    restaurantOrderDecisionResponse.getSagaId());
            return true;
        }

        return false;
    }

    private void acceptOrder(Order order) {
        order.accept();
        log.info("주문 접수 완료. Order Id : {}", order.getId());
    }

    private void updateAcceptOutbox(RestaurantAcceptOutbox restaurantAcceptOutbox,
                                    OrderStatus orderStatus, SagaStatus sagaStatus) {
        restaurantAcceptOutbox.updateProcessedAt(ZonedDateTime.now());
        restaurantAcceptOutbox.updateOrderStatus(orderStatus);
        restaurantAcceptOutbox.updateSagaStatus(sagaStatus);
    }

    private void updatePaymentOutbox(UUID sagaId, OrderStatus orderStatus, SagaStatus sagaStatus) {
        Optional<PaymentOutbox> paymentOutboxResponse =
                paymentOutboxHelper.getPaymentOutboxBySagaIdAndSagaStatus(sagaId, SagaStatus.PROCESSING);

        if (paymentOutboxResponse.isEmpty()) {
            throw new OrderApplicationException(
                    "SagaStatus가 " + SagaStatus.PROCESSING.name() + " 상태인 PaymentOutbox를 찾지 못했습니다.");
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
