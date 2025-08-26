package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.response.OrderStatusResponse;
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
@RequiredArgsConstructor
@Slf4j
public class OrderTrackingService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderStatusResponse trackOrder(UUID trackingId) {
        Optional<Order> order = orderRepository.findByTrackingId(trackingId);

        if (order.isEmpty()) {
            log.warn("trackingId에 대한 주문을 찾을 수 없습니다. trackingId : {}", trackingId);
            throw new OrderNotFoundException("trackingId에 대한 주문을 찾을 수 없습니다. trackingId : " + trackingId);
        }
        return OrderStatusResponse.builder()
                .orderTrackingId(order.get().getTrackingId())
                .orderStatus(order.get().getOrderStatus())
                .failureMessages(order.get().getFailureMessageList())
                .build();
    }

}
