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
        RestaurantInfo restaurantInfo = restaurantApi.getRestaurantInfo(createOrderRequest.getRestaurantId(),
                orderDataMapper.itemsToItemIdList(createOrderRequest.getItems()));

        OrderCreateEvent orderCreateEvent = orderCreateHelper.persistOrder(createOrderRequest, restaurantInfo);
        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());

        UUID sagaId = UUID.randomUUID();
        paymentOutboxHelper.savePaymentOutboxMessage(
                orderDataMapper.orderCreatedToOrderPaymentEventPayload(orderCreateEvent, sagaId),
                orderCreateEvent.getOrder().getOrderStatus(),
                OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCreateEvent.getOrder().getOrderStatus()),
                OutboxStatus.STARTED,
                sagaId
        );

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), "주문이 성공적으로 생성되었습니다.");
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
