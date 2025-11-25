package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.SagaStatus;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.domain.event.OrderRejectedEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.outbox.MessageType;
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
public class OrderRestaurantApprovalService {

    private final OrderRepository orderRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Transactional
    public void approve(RestaurantOrderDecisionResponse response) {
        if (checkAndMarkProcessed(response, MessageType.RESTAURANT_APPROVAL)) {
            return;
        }

        Order order = findOrder(response.getOrderId());
        order.approve();

        log.info("주문이 승인되었습니다. Order Id : {}", response.getOrderId());
    }

    @Transactional
    public void rejecting(RestaurantOrderDecisionResponse response) {
        log.info("레스토랑 주문 거절 처리 시작. Order Id : {}", response.getOrderId());
        if (checkAndMarkProcessed(response, MessageType.RESTAURANT_REJECT)) {
            return;
        }

        Order order = findOrder(response.getOrderId());
        OrderRejectedEvent orderRejectedEvent = order.rejecting();

        SagaStatus sagaStatus = OrderStatusToSagaStatus.orderStatusToSagaStatus(orderRejectedEvent.getOrder()
                .getOrderStatus());

        paymentOutboxHelper.savePaymentOutboxMessage(
                orderDataMapper.orderRejectedEventToOrderPaymentEventPayload(orderRejectedEvent, response.getSagaId()),
                orderRejectedEvent.getOrder().getOrderStatus(),
                sagaStatus,
                response.getSagaId()
        );

        log.info("레스토랑 거절로 인해 주문을 rejecting 처리했습니다. order Id : {}", orderRejectedEvent.getOrder().getId());
    }

    @Transactional
    public void reject(PaymentResponse response) {
        Order order = findOrder(response.getOrderId());
        order.reject();
        log.info("주문 reject 처리 완료. orderId : {}", response.getOrderId());
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

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문 정보를 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문 정보를 찾을 수 없습니다.");
        }
        return order.get();
    }

}
