package com.orderingsystem.order.application;

import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
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

    public CreateOrderResponse createOrder(CreateOrderApplicationRequest createOrderRequest) {
        OrderCreateEvent orderCreateEvent = orderCreateHelper.persistOrder(createOrderRequest);
        log.info("주문이 생성되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());
        orderCreatedPaymentRequestMessagePublisher.publish(orderCreateEvent);

        return orderDataMapper.orderToCreateOrderResponse(orderCreateEvent.getOrder(), "주문이 성공적으로 생성되었습니다.");
    }
}
