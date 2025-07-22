package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.dto.response.OrderStatusResponse;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.dto.response.RestaurantApprovalResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderCreateHelper orderCreateHelper;
    private final OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestMessagePublisher;
    private final OrderDataMapper orderDataMapper;
    private final OrderPaymentService orderPaymentService;
    private final OrderApprovalService orderApprovalService;
    private final OrderRepository orderRepository;
    private final RestaurantApi restaurantApi;

    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        RestaurantInfo restaurantInfo = restaurantApi.getRestaurantInfo(createOrderRequest.getRestaurantId(),
                orderDataMapper.itemsToItemIdList(createOrderRequest.getItems()));

        OrderCreateEvent orderCreateEvent = orderCreateHelper.persistOrder(createOrderRequest, restaurantInfo);

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

    @Transactional(readOnly = true)
    public OrderStatusResponse trackOrder(UUID trackingId) {
        Optional<Order> order = orderRepository.findByTrackingId(trackingId);

        if (order.isEmpty()) {
            log.warn("trackingId에 대한 주문을 찾을 수 없습니다. trackingId : {}", trackingId);
            throw new OrderNotFoundException("trackingId에 대한 주문을 찾을 수 없습니다. trackingId : " + trackingId);
        }
        return OrderStatusResponse.builder()
                .orderTrackingId(order.get().getTrackingId())
                .orderStatus(order.get().getOrderStatus())
                .failureMessages(order.get().getFailureMessageList())
                .build();
    }
}
