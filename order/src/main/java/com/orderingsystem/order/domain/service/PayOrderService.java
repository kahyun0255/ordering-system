package com.orderingsystem.order.domain.service;

import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.model.Order;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PayOrderService {

    public OrderPaidEvent payOrder(Order order){
        order.pay();
        log.info("주문 결제가 완료되었습니다. Order Id : {}", order.getId());
        return new OrderPaidEvent(order, ZonedDateTime.now());
    }
}
