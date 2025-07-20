package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.EmptyEvent;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.application.publisher.OrderCancelledPaymentRequestMessagePublisher;
import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.restaurant.domain.service.OrderPaymentCancelService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderApprovalService implements SagaStep<RestaurantApprovalResponse, EmptyEvent, OrderCancelledEvent> {

    private final OrderRepository orderRepository;
    private final OrderPaymentCancelService orderPaymentCancelService;
    private final OrderCancelledPaymentRequestMessagePublisher orderCancelledPaymentRequestMessagePublisher;

    @Override
    @Transactional
    public EmptyEvent process(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("주문 승인 처리 중. Order Id : {}", restaurantApprovalResponse.getOrderId());

        Order order = findOrder(restaurantApprovalResponse.getOrderId());
        order.approve();
        orderRepository.save(order);

        return EmptyEvent.INSTANCE;
    }

    @Override
    @Transactional
    public OrderCancelledEvent rollback(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("주문 취소 처리 중. Order Id : {}", restaurantApprovalResponse.getOrderId());

        Order order = findOrder(restaurantApprovalResponse.getOrderId());
        OrderCancelledEvent orderCancelledEvent = orderPaymentCancelService.cancelOrderPayment(order,
                restaurantApprovalResponse.getFailureMessages(),
                orderCancelledPaymentRequestMessagePublisher);
        log.info("레스토랑 승인 거절로 인해 주문 ID: {} 의 주문을 취소 처리합니다", order.getId());

        return orderCancelledEvent;
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
