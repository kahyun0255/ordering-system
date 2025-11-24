package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final RestaurantApi restaurantApi;
    private final OrderCreateService orderCreateService;
    private final OrderCancelService orderCancelService;
    private final OrderService orderService;
    private final OrderDataMapper orderDataMapper;

    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        List<String> failureMessages = new ArrayList<>();

        UUID sagaId = UUID.randomUUID();
        restaurantApi.validRestaurantAndProducts(createOrderRequest, sagaId);

        orderService.checkCustomer(createOrderRequest.getCustomerId());

        OrderCreateEvent orderCreateEvent = orderCreateService.createOrder(createOrderRequest, failureMessages, sagaId);

        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());

        String resultMessage = "주문이 성공적으로 생성되었습니다.";
        if (!failureMessages.isEmpty()) {
            resultMessage = "주문이 취소되었습니다.";
        }

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), resultMessage);
    }

    public void cancelOrder(UUID trackingId, UUID userId) {
        log.info("[{}] 유저가 [{}] trackingId에 해당하는 주문을 취소.", userId, trackingId);

        orderService.checkCustomer(userId);
        orderCancelService.cancelOrder(trackingId, userId);
    }

}
