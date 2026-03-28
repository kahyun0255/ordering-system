package com.orderingsystem.order.presentation;

import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.order.application.OrderFacade;
import com.orderingsystem.order.application.OrderTrackingService;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.dto.response.OrderStatusResponse;
import com.orderingsystem.order.presentation.request.CreateOrderRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final CommonJwtUtil commonJwtUtil;
    private final OrderFacade orderFacade;
    private final OrderTrackingService orderTrackingService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest createOrderRequest,
                                                           @RequestHeader("Authorization") String authorizationHeader) {
        UUID customerId = commonJwtUtil.getUserIdFromToken(authorizationHeader);

        log.info("주문 생성 시작 customer : {}, restaurant : {}", customerId, createOrderRequest.getRestaurantId());

        CreateOrderResponse createOrderResponse =
                orderFacade.createOrder(createOrderRequest.toApplicationRequest(customerId));

        log.info("주문 생성 완료 tracking id : {}", createOrderResponse.getOrderTrackingId());
        return ResponseEntity.ok(createOrderResponse);
    }

    @GetMapping("/{trackingId}")
    public ResponseEntity<OrderStatusResponse> getOrderByTrackingId(@PathVariable UUID trackingId) {
        log.info("주문 추적 시작 : {}", trackingId);
        return ResponseEntity.ok(orderTrackingService.trackOrder(trackingId));
    }

    @PostMapping("/{trackingId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID trackingId,
                                            @RequestHeader("Authorization") String authorizationHeader) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        orderFacade.cancelOrder(trackingId, userId);
        return ResponseEntity.noContent().build();
    }

}
