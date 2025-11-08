package com.orderingsystem.restaurant.presentation;

import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.restaurant.application.OrderApprovalFacade;
import com.orderingsystem.restaurant.application.OrderFacade;
import com.orderingsystem.restaurant.application.dto.response.OrderResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class OrderController {

    private final CommonJwtUtil commonJwtUtil;
    private final OrderApprovalFacade orderApprovalFacade;
    private final OrderFacade orderFacade;

    @PostMapping("/{orderId}/approve")
    public ResponseEntity<Void> approve(@RequestHeader(value = "Authorization") String authorizationHeader,
                                        @PathVariable UUID restaurantId, @PathVariable UUID orderId) {
        UUID ownerId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        orderApprovalFacade.approve(orderId, restaurantId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(@RequestHeader(value = "Authorization") String authorizationHeader,
                                                         @PathVariable UUID restaurantId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String status) {
        UUID ownerId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        PageRequest pageRequest = PageRequest.of(page, size);
        return ResponseEntity.ok(orderFacade.getOrders(restaurantId, ownerId, pageRequest, status));
    }

}
