package com.orderingsystem.order.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancelService {

    private final OrderRepository orderRepository;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Transactional
    public void cancelOrder(UUID trackingId, UUID userId) {
        List<String> failureMessages = new ArrayList<>();
        UUID sagaId = UUID.randomUUID();

        Order order = findOrderByTrackingId(trackingId, userId);
        verifyCustomerOwnership(trackingId, userId, order);
        OrderCancelledEvent orderCancelledEvent = order.requestCancelByCustomer(failureMessages);

        if (failureMessages.isEmpty()) {
            log.info("[{}] 주문에 대한 취소를 성공적으로 접수했습니다.", order.getId());

            paymentOutboxHelper.savePaymentOutboxMessage(
                    orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(orderCancelledEvent, sagaId),
                    orderCancelledEvent.getOrder().getOrderStatus(),
                    OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCancelledEvent.getOrder().getOrderStatus()),
                    sagaId
            );
        } else {
            log.info("[{}] 주문에 대한 취소를 실패했습니다. trackingId = [{}], userId = [{}]", order.getId(), trackingId, userId);
        }
    }

    private Order findOrderByTrackingId(UUID trackingId, UUID userId) {
        Optional<Order> order = orderRepository.findByTrackingId(trackingId);
        if (order.isEmpty()) {
            log.info("[{}] trackingId에 해당하는 주문이 존재하지 않습니다. 조회한 유저 id : [{}]", trackingId, userId);
            throw new OrderNotFoundException("주문 내역을 찾을 수 없습니다.");
        }
        return order.get();
    }

    private void verifyCustomerOwnership(UUID trackingId, UUID userId, Order order) {
        if (!order.getCustomerId().equals(userId)) {
            log.info("[{}] 유저는 [{}] 주문을 취소할 권한이 없습니다. trackingId : [{}]", userId, order, trackingId);
            throw new AccessDeniedException("주문을 취소할 권한이 없습니다.");
        }
    }

}
