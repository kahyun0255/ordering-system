package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.response.OrderResponse;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final RestaurantAccessValidatorService restaurantAccessValidatorService;
    private final OrderService orderService;

    public Page<OrderResponse> getOrders(UUID restaurantId, UUID ownerId, PageRequest pageRequest, String status) {
        if (!restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)) {
            log.info("[{}] 유저는 [{}] 레스토랑의 주문 정보를 확인할 권한이 없습니다.", ownerId, restaurantId);
            throw new AccessDeniedException("주문 정보를 확인할 권한이 없습니다.");
        }

        Restaurant restaurant = restaurantAccessValidatorService.findRestaurant(restaurantId);
        if (!restaurant.getStatus().canAccessOrders()) {
            log.info("[{}] 레스토랑은 주문 정보를 조회할 수 없는 상태입니다. 상태 : {}", restaurantId, restaurant.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "레스토랑이 주문 정보를 조회할 수 없는 상태입니다.");
        }

        return orderService.findOrders(restaurantId, pageRequest, status);
    }

}
