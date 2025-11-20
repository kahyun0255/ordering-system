package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantStatusValidatorService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderApprovalFacade {

    private final RestaurantAccessValidatorService restaurantAccessValidatorService;
    private final OrderApprovalService orderApprovalService;
    private final RestaurantStatusValidatorService restaurantStatusValidatorService;
    private final RestaurantRepository restaurantRepository;
    private final InventoryFacade inventoryFacade;

    public void approve(UUID orderId, UUID restaurantId, UUID ownerId) {
        log.info("[{}] 유저가 [{}] 레스토랑의 [{}] 주문 승인 처리 시작.", ownerId, restaurantId, orderId);


        if (!restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)) {
            throw new AccessDeniedException("해당 레스토랑의 주문을 승인할 권한이 없습니다.");
        }

        Restaurant restaurant = getRestaurant(restaurantId);
        restaurantStatusValidatorService.validateActive(restaurant, orderId);

        orderApprovalService.approval(restaurantId, orderId, ownerId);
    }

    public void reject(UUID orderId, UUID restaurantId, UUID ownerId) {
        log.info("[{}] 유저가 [{}] 레스토랑의 [{}] 주문을 거절.", ownerId, restaurantId, ownerId);

        if (!restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)) {
            throw new AccessDeniedException("해당 레스토랑의 주문을 거절할 권한이 없습니다.");
        }

        Restaurant restaurant = getRestaurant(restaurantId);
        restaurantStatusValidatorService.validateActive(restaurant, orderId);

        orderApprovalService.reject(restaurantId, orderId, ownerId);

        inventoryFacade.cancel(orderId);
    }

    private Restaurant getRestaurant(UUID restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);

        if (restaurant.isEmpty() || restaurant.get().getStatus().equals(RestaurantStatus.DELETED)) {
            throw new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
        }
        return restaurant.get();
    }
}
