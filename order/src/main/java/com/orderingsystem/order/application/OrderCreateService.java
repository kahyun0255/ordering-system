package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.mapper.OrderDataMapper;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Customer;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderAddress;
import com.orderingsystem.order.domain.repository.CustomerRepository;
import com.orderingsystem.order.domain.repository.OrderAddressRepository;
import com.orderingsystem.order.domain.repository.OrderRepository;
import com.orderingsystem.order.domain.service.OrderValidateAndInitiateService;
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
public class OrderCreateService {

    private final OrderDataMapper orderDataMapper;
    private final OrderRepository orderRepository;
    private final OrderValidateAndInitiateService orderValidateAndInitiateService;
    private final CustomerRepository customerRepository;
    private final OrderAddressRepository orderAddressRepository;

    @Transactional
    public OrderCreateEvent createOrder(CreateOrderApplicationRequest createOrderRequest, List<String> failureMessages) {
        checkCustomer(createOrderRequest.getCustomerId());

        OrderAddress orderAddress = orderDataMapper.orderAddressToStreetAddress(createOrderRequest.getAddress());
        Order order = orderDataMapper.createOrderRequestToOrder(createOrderRequest, orderAddress.getId());

        OrderCreateEvent orderCreateEvent = orderValidateAndInitiateService.validateAndInitiate(order, failureMessages);

        Order savedOrder = saveOrder(order);
        saveOrderAddress(orderAddress, order);

        if (failureMessages.isEmpty()) {
            log.info("주문이 생성되었습니다. Order Id : {}", savedOrder.getId());
        }else {
            log.warn("주문이 취소되었습니다. Order Id : {}", orderCreateEvent.getOrder().getId());
            order.cancel(failureMessages);
        }

        return orderCreateEvent;
    }

    private void checkCustomer(UUID customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty()) {
            log.warn("주문자를 찾을 수 없습니다. Customer Id : {}", customerId);
            throw new OrderNotFoundException("주문자를 찾을 수 없습니다.");
        }
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
