package com.orderingsystem.order.domain.service;

import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderPaymentCancelService {

    public OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages) {
        order.initCancel(failureMessages);
        log.info("주문 결제 취소 진행 중. Order Id : {}", order.getId());
        return new OrderCancelledEvent(order, ZonedDateTime.now());
    }
}
