package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.application.outbox.coupon.CouponOutboxHelper;
import com.orderingsystem.order.application.outbox.payment.PaymentOutboxHelper;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderAddress;
import com.orderingsystem.order.domain.repository.OrderAddressRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.OrderValidateAndInitiateService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreateService {

    private final OrderDataMapper orderDataMapper;
    private final OrderRepository orderRepository;
    private final OrderValidateAndInitiateService orderValidateAndInitiateService;
    private final OrderAddressRepository orderAddressRepository;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final CouponOutboxHelper couponOutboxHelper;

    @Transactional
    public OrderCreateEvent createOrder(CreateOrderApplicationRequest createOrderRequest, List<String> failureMessages,
                                        UUID sagaId, List<Long> couponId) {
        OrderAddress orderAddress = orderDataMapper.orderAddressToStreetAddress(createOrderRequest.getAddress());
        Order order = orderDataMapper.createOrderRequestToOrder(createOrderRequest, orderAddress.getId());

        OrderCreateEvent orderCreateEvent = orderValidateAndInitiateService.validateAndInitiate(order, failureMessages);

        Order savedOrder = saveOrder(order);
        saveOrderAddress(orderAddress, order);

        if (failureMessages.isEmpty()) {
            paymentOutboxHelper.savePaymentOutboxMessage(
                    orderDataMapper.orderCreatedToOrderPaymentEventPayload(orderCreateEvent, sagaId),
                    orderCreateEvent.getOrder().getOrderStatus(),
                    OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCreateEvent.getOrder().getOrderStatus()),
                    sagaId
            );

            if (couponId != null && !couponId.isEmpty()) {
                couponOutboxHelper.saveCouponOutboxMessage(
                        orderDataMapper.orderCreatedToOrderCouponEventPayload(orderCreateEvent, sagaId, couponId),
                        orderCreateEvent.getOrder().getOrderStatus(),
                        OrderStatusToSagaStatus.orderStatusToSagaStatus(orderCreateEvent.getOrder().getOrderStatus()),
                        sagaId
                );
            }
            log.info("주문이 생성되었습니다. Order Id : {}", savedOrder.getId());
        } else {
            log.warn("주문이 취소되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());
            order.cancel(failureMessages);
        }

        return orderCreateEvent;
    }

    private Order saveOrder(Order order) {
        try {
            Order savedOrder = orderRepository.save(order);
            log.info("주문이 저장되었습니다. Order Id : {}", savedOrder.getId());
            return savedOrder;
        } catch (Exception e) {
            log.warn("주문이 저장되지 않았습니다.");
            throw new OrderDomainException("주문이 저장되지 않았습니다.");
        }
    }

    private void saveOrderAddress(OrderAddress orderAddress, Order order) {
        try {
            orderAddress.updateOrderId(order.getId());
            orderAddressRepository.save(orderAddress);
        } catch (Exception e) {
            log.warn("주문 주소가 저장되지 않았습니다.");
            throw new OrderDomainException("주문 주소가 저장되지 않았습니다.");
        }
    }
}
