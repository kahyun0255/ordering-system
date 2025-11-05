package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.restaurant.application.OrderApprovalFacade;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderApprovalController {

    private final CommonJwtUtil commonJwtUtil;
    private final OrderApprovalFacade orderApprovalFacade;

    @PostMapping("/{orderId}/approve")
    public ResponseEntity<Void> approve(@RequestHeader(value = "Authorization")String authorizationHeader,
                                        @PathVariable UUID restaurantId, @PathVariable UUID orderId){
        UUID ownerId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        orderApprovalFacade.approve(orderId, restaurantId, ownerId);
        return ResponseEntity.noContent().build();
    }

}
