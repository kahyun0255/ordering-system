package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRestaurantApprovalService {

    private final OrderApprovalService orderApprovalService;

    public void orderApprove(RestaurantApprovalResponse restaurantApprovalResponse) {
        orderApprovalService.process(restaurantApprovalResponse);
        log.info("주문이 승인되었습니다. Order Id : {}", restaurantApprovalResponse.getOrderId());
    }

    public void orderReject(RestaurantApprovalResponse restaurantApprovalResponse) {
        orderApprovalService.rollback(restaurantApprovalResponse);

        log.info("레스토랑 승인 거절로 인해 주문 ID: {} 의 주문을 취소 처리했습니다. failureMessages : {}",
                restaurantApprovalResponse.getOrderId(),
                String.join(",", restaurantApprovalResponse.getFailureMessages()));
    }
}
