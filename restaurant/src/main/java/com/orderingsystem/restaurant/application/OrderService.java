package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.restaurant.application.dto.response.OrderResponse;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderApprovalRepository orderApprovalRepository;

    public Page<OrderResponse> findOrders(UUID restaurantId, PageRequest pageRequest, String status) {
        Page<OrderApproval> page;

        if (status == null || status.isBlank()) {
            page = orderApprovalRepository.findByRestaurantId(restaurantId, pageRequest);
        } else {
            OrderApprovalStatus statusEnum;
            try {
                statusEnum = OrderApprovalStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("지원하지 않는 주문 상태입니다.");
            }

            page = orderApprovalRepository.findByRestaurantIdAndStatus(restaurantId, statusEnum, pageRequest);
        }

        return page.map(OrderResponse::from);
    }

}
