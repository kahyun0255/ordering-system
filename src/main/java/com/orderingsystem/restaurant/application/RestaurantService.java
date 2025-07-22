package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantApprovalRequestHelper restaurantApprovalRequestHelper;

    public void approveOrder(ApprovalRequest approvalRequest) {
        OrderApprovalEvent orderApprovalEvent = restaurantApprovalRequestHelper.persistOrderApproval(approvalRequest);
        orderApprovalEvent.fire();
    }
}
