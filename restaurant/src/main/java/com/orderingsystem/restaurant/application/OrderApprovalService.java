package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovedEvent;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderRejectedEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderApprovalService {

    private final OrderApprovalRepository orderApprovalRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional
    public void approval(UUID restaurantId, UUID orderId, UUID ownerId) {
        OrderApproval orderApproval = getOrderApproval(orderId);
        OrderApprovedEvent orderApprovedEvent = orderApproval.approval();

        UUID sagaId = UUID.randomUUID();
        orderOutboxHelper.saveOrderOutboxMessage(
                restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(orderApprovedEvent),
                orderApprovedEvent.getOrderApproval().getStatus(),
                sagaId
        );

        log.info("[{}] 유저가 [{}] 레스토랑의 [{}] 주문 승인 완료.", ownerId, restaurantId, orderId);
    }

    @Transactional
    public void reject(UUID restaurantId, UUID orderId, UUID ownerId) {
        OrderApproval orderApproval = getOrderApproval(orderId);
        OrderRejectedEvent orderRejectedEvent = orderApproval.reject();

        UUID sagaId = UUID.randomUUID();
        orderOutboxHelper.saveOrderOutboxMessage(
                restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(orderRejectedEvent),
                orderRejectedEvent.getOrderApproval().getStatus(),
                sagaId
        );

        log.info("[{}] 유저가 [{}] 레스토랑의 [{}] 주문 거절 완료.", ownerId, restaurantId, orderId);
    }

    private OrderApproval getOrderApproval(UUID orderId){
        return orderApprovalRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RestaurantNotFoundException("주문 내역을 찾을 수 없습니다."));
    }

}
