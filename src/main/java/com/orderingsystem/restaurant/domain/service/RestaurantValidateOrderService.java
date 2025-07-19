package com.orderingsystem.restaurant.domain.service;

import com.orderingsystem.common.domain.publisher.DomainEventPublisher;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.event.OrderApprovedEvent;
import com.orderingsystem.restaurant.domain.event.OrderRejectedEvent;
import com.orderingsystem.restaurant.domain.model.RestaurantVO;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RestaurantValidateOrderService {

    public OrderApprovalEvent validateOrder(RestaurantVO restaurant,
                                            List<String> failureMessages,
                                            DomainEventPublisher<OrderApprovedEvent> orderApprovedEventDomainEventPublisher,
                                            DomainEventPublisher<OrderRejectedEvent> orderRejectedEventDomainEventPublisher) {
        restaurant.validateOrder(failureMessages);
        log.info("주문 검증 시작. Order Id : {}", restaurant.getOrderDetail().getOrderId());

        if (failureMessages.isEmpty()) {
            log.info("주문이 승인되었습니다. Order Id : {}", restaurant.getOrderDetail().getOrderId());
            restaurant.constructOrderApproval(OrderApprovalStatus.APPROVED);

            return new OrderApprovedEvent(restaurant.getOrderApproval(), restaurant.getRestaurantId(), failureMessages,
                    ZonedDateTime.now(), orderApprovedEventDomainEventPublisher);
        } else {
            log.info("주문이 거절되었습니다. Order Id : {}", restaurant.getOrderDetail().getOrderId());
            restaurant.constructOrderApproval(OrderApprovalStatus.REJECTED);

            return new OrderRejectedEvent(restaurant.getOrderApproval(), restaurant.getRestaurantId(), failureMessages,
                    ZonedDateTime.now(), orderRejectedEventDomainEventPublisher);
        }
    }
}
