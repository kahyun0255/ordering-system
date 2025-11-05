package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.response.RestaurantOrderDecisionResponse;
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

    @Transactional
    public void approve(RestaurantOrderDecisionResponse restaurantOrderDecisionResponse) {
        if (checkAndMarkProcessed(restaurantOrderDecisionResponse, MessageType.RESTAURANT_APPROVAL)) {
            return;
        }

        Order order = findOrder(restaurantOrderDecisionResponse.getOrderId());
        order.approve();

        log.info("주문이 승인되었습니다. Order Id : {}", restaurantOrderDecisionResponse.getOrderId());
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
            throw new OrderNotFoundException("주문 정보를 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }

}
