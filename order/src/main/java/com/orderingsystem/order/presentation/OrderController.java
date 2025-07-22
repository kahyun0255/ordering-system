package com.orderingsystem.order.presentation;

import com.orderingsystem.order.application.OrderService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        log.info("주문 생성 시작 customer : {}, restaurant : {}",
                createOrderRequest.getCustomerId(), createOrderRequest.getRestaurantId());

        CreateOrderResponse createOrderResponse = orderService.createOrder(createOrderRequest.toApplicationRequest());

        log.info("주문 생성 완료 tracking id : {}", createOrderResponse.getOrderTrackingId());
        return ResponseEntity.ok(createOrderResponse);
    }

    @GetMapping("/{trackingId}")
    public ResponseEntity<OrderStatusResponse> getOrderByTrackingId(@PathVariable UUID trackingId) {
        log.info("주문 추적 시작 : {}", trackingId);
        return ResponseEntity.ok(orderService.trackOrder(trackingId));
    }
}
