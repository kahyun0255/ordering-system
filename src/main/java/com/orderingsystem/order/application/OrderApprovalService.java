package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.EmptyEvent;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
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

    @Override
    @Transactional
    public EmptyEvent process(RestaurantApprovalResponse restaurantApprovalResponse) {
        log.info("주문 승인 처리 중. Order Id : {}", restaurantApprovalResponse.getOrderId());

        Order order = findOrder(restaurantApprovalResponse.getOrderId());
        order.approve();
        orderRepository.save(order);

        return EmptyEvent.INSTANCE;
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문 정보를 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문 정보를 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }

    @Override
    public OrderCancelledEvent rollback(RestaurantApprovalResponse restaurantApprovalResponse) {
        return null;
    }
}
