package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderApprovalFacade {

    private final RestaurantOwnershipRepository restaurantOwnershipRepository;
    private final OrderApprovalService orderApprovalService;

    public void approve(UUID orderId, UUID restaurantId, UUID ownerId) {
        log.info("[{}] 유저가 [{}] 레스토랑의 [{}] 주문 승인 처리 시작.", ownerId, restaurantId, orderId);

        if (!isOwnership(restaurantId, ownerId)){
            throw new AccessDeniedException("해당 레스토랑의 주문을 승인할 권한이 없습니다.");
        }

        orderApprovalService.approval(restaurantId, orderId, ownerId);
    }

    private boolean isOwnership(UUID restaurantId, UUID ownerId){
        Optional<RestaurantOwnership> restaurantOwnership = restaurantOwnershipRepository.findByOwnerIdAndRestaurantId(
                ownerId, restaurantId);

        return restaurantOwnership.isPresent();
    }

}
