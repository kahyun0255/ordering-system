package com.orderingsystem.order.application;

import com.orderingsystem.common.saga.EmptyEvent;
import com.orderingsystem.common.saga.SagaStep;
import com.orderingsystem.order.application.dto.response.PaymentResponse;
import com.orderingsystem.order.application.publisher.OrderPaidRestaurantRequestMassagePublisher;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.PayOrderService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService implements SagaStep<PaymentResponse, OrderPaidEvent, EmptyEvent> {

    private final OrderRepository orderRepository;
    private final PayOrderService payOrderService;
    private final OrderPaidRestaurantRequestMassagePublisher orderPaidRestaurantRequestMassagePublisher;

    @Override
    @Transactional
    public OrderPaidEvent process(PaymentResponse paymentResponse) {
        log.info("해당 주문의 결제 처리를 시작합니다. Order Id : {}", paymentResponse.getOrderId());

        Order order = findOrder(paymentResponse.getOrderId());
        OrderPaidEvent domainEvent = payOrderService.payOrder(order, orderPaidRestaurantRequestMassagePublisher);
        orderRepository.save(order);

        log.info("해당 주문의 결제 처리가 성공적으로 완료되었습니다. Order Id : {} ", order.getId());
        return domainEvent;
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);

        if (order.isEmpty()) {
            log.error("주문을 찾을 수 없습니다. Order Id : {}", orderId);
            throw new OrderNotFoundException("주문을 찾을 수 없습니다. Order Id : " + orderId);
        }
        return order.get();
    }

    @Override
    public EmptyEvent rollback(PaymentResponse paymentResponse) {
        return null;
    }
}
