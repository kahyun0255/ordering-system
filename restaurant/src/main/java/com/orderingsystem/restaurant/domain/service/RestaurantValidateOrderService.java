package com.orderingsystem.restaurant.domain.service;

import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.model.RestaurantInfo;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RestaurantValidateOrderService {

    public OrderApprovalEvent validateOrder(RestaurantInfo restaurant, List<String> failureMessages, UUID sagaId) {
        log.info("주문 검증 시작. Order Id : {}", restaurant.getOrderDetail().getOrderId());
        restaurant.validateOrder(failureMessages);

        if (failureMessages.isEmpty()) {
            return restaurant.acceptOrder(failureMessages, sagaId);
        } else {
            return restaurant.rejectOrder(failureMessages, sagaId);
        }
    }
}
