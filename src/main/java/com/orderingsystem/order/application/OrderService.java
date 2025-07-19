package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.publisher.OrderCreatedPaymentRequestMessagePublisher;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
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
}
