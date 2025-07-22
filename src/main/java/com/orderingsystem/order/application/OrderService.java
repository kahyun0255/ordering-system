package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderCreateHelper orderCreateHelper;
    private final OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestMessagePublisher;
    private final OrderDataMapper orderDataMapper;
    private final OrderPaymentService orderPaymentService;
    private final OrderApprovalService orderApprovalService;

    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        OrderCreateEvent orderCreateEvent = orderCreateHelper.persistOrder(createOrderRequest);
        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());
        orderCreatedPaymentRequestMessagePublisher.publish(orderCreateEvent);

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), "주문이 성공적으로 생성되었습니다.");
    }

    public void completePayment(PaymentResponse paymentResponse) {
        OrderPaidEvent orderPaidEvent = orderPaymentService.process(paymentResponse);
        log.info("OrderPaidEvent 발행. order Id : {}", orderPaidEvent.getOrder().getId());
        orderPaidEvent.fire();
    }

    public void paymentCancelled(PaymentResponse paymentResponse) {
        orderPaymentService.rollback(paymentResponse);
        log.info("주문 rollback. Order Id : {}, failureMassages : {} ",
                paymentResponse.getOrderId(), String.join(",", paymentResponse.getFailureMessages()));
    }

    public void orderApprove(RestaurantApprovalResponse restaurantApprovalResponse) {
        orderApprovalService.process(restaurantApprovalResponse);
        log.info("주문이 승인되었습니다. Order Id : {}", restaurantApprovalResponse.getOrderId());
    }

    public void orderReject(RestaurantApprovalResponse restaurantApprovalResponse) {
        OrderCancelledEvent orderCancelledEvent = orderApprovalService.rollback(restaurantApprovalResponse);

        log.info("레스토랑 승인 거절로 인해 주문 ID: {} 의 주문을 취소 처리했습니다. failureMessages : {}",
                restaurantApprovalResponse.getOrderId(),
                String.join(",", restaurantApprovalResponse.getFailureMessages()));

        orderCancelledEvent.fire();
    }
}
