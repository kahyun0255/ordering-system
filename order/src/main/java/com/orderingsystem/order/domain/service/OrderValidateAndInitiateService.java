package com.orderingsystem.order.domain.service;

import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderValidateAndInitiateService {

    public OrderCreateEvent validateAndInitiate(Order order, List<String> failureMessages) {
        order.validateOrder(failureMessages);
        order.initializeOrder();

        log.info("주문 생성. Order Id : {}", order.getId());

        return new OrderCreateEvent(order, ZonedDateTime.now());
    }

}
