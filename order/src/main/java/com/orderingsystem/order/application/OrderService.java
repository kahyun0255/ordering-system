package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.dto.response.OrderStatusResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.outbox.OutboxStatus;
import java.util.ArrayList;
import java.util.List;
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
    private final OrderDataMapper orderDataMapper;
    private final OrderRepository orderRepository;
    private final RestaurantApi restaurantApi;
    private final PaymentOutboxHelper paymentOutboxHelper;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        List<String> failureMessages = new ArrayList<>();

        RestaurantInfo restaurantInfo = restaurantApi.getRestaurantInfo(createOrderRequest.getRestaurantId(),
                orderDataMapper.itemsToItemIdList(createOrderRequest.getItems()));

        OrderCreateEvent orderCreateEvent = orderCreateHelper.persistOrder(createOrderRequest, restaurantInfo,
                failureMessages);
        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());

        String resultMessage = "주문이 성공적으로 생성되었습니다.";

        if (!failureMessages.isEmpty()) {
            log.warn("주문이 취소되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());
            Order order = orderCreateEvent.getOrder();
            order.cancel(failureMessages);
            orderRepository.save(order);
            resultMessage = "주문 요청이 유효하지 않아 주문이 완료되지 않았습니다.";
        } else {
            UUID sagaId = UUID.randomUUID();
            paymentOutboxHelper.savePaymentOutboxMessage(
                    orderDataMapper.orderCreatedToOrderPaymentEventPayload(orderCreateEvent, sagaId),
                    orderCreateEvent.getOrder().getOrderStatus(),
                    OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCreateEvent.getOrder().getOrderStatus()),
                    OutboxStatus.STARTED,
                    sagaId
            );
        }

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), resultMessage);
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
