package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CouponValidationResponse;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.port.out.CouponApi;
import com.orderingsystem.order.application.port.out.RestaurantApi;
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
    private final CouponApi couponApi;

    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        List<String> failureMessages = new ArrayList<>();

        UUID sagaId = UUID.randomUUID();

        restaurantApi.validRestaurantAndProducts(createOrderRequest, sagaId);

        CouponValidationResponse couponValidationResponse = null;
        if (createOrderRequest.getCouponId() != null && !createOrderRequest.getCouponId().isEmpty()) {
            couponValidationResponse = couponApi.validateCoupons(
                    orderDataMapper.createOrderRequestToValidationCouponApplicationRequest(createOrderRequest), sagaId);

            checkCouponValidity(couponValidationResponse, failureMessages, createOrderRequest.getCustomerId(), sagaId);
        }

        orderService.checkCustomer(createOrderRequest.getCustomerId());

        OrderCreateEvent orderCreateEvent = orderCreateService.createOrder(createOrderRequest, failureMessages, sagaId,
                createOrderRequest.getCouponId(), couponValidationResponse);

        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());

        String resultMessage = "주문이 성공적으로 생성되었습니다.";
        if (!failureMessages.isEmpty()) {
            resultMessage = "주문이 취소되었습니다.";
        }

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), resultMessage);
    }

    private void checkCouponValidity(CouponValidationResponse couponValidationResponse, List<String> failureMessages,
                                     UUID userId, UUID sagaId) {
        if (!couponValidationResponse.isValid()) {
            log.info("[{}] 유저의 주문 쿠폰 검증 실패 : {}, SagaId : [{}]", userId, couponValidationResponse.getMessage(), sagaId);
            failureMessages.add(couponValidationResponse.getMessage());
        }
    }

    public void cancelOrder(UUID trackingId, UUID userId) {
        log.info("[{}] 유저가 [{}] trackingId에 해당하는 주문을 취소.", userId, trackingId);

        orderService.checkCustomer(userId);
        orderCancelService.cancelOrder(trackingId, userId);
    }

}
