package com.orderingsystem.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.application.dto.response.OrderStatusResponse;
import com.orderingsystem.order.domain.exception.OrderNotFoundException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OrderTrackingServiceTest {

    @Autowired
    private OrderTrackingService orderTrackingService;

    @Autowired
    private OrderRepository orderRepository;

    @DisplayName("Tracking Id로 주문 상태 조회에 성공한다.")
    @Test
    void trackOrder () {
        //given
        OrderStatus orderStatus = OrderStatus.PENDING;
        UUID trackingId = UUID.randomUUID();
        String failureMessage = "실패했습니다,금액이 일치하지 않습니다";
        List<String> failureMessages = List.of("실패했습니다","금액이 일치하지 않습니다");

        orderRepository.save(Order.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .trackingId(trackingId)
                .address(UUID.randomUUID())
                .price(new Money(new BigDecimal("20.00")))
                .items(List.of())
                .orderStatus(orderStatus)
                .failureMessages(failureMessage)
                .build());

        //when
        OrderStatusResponse response = orderTrackingService.trackOrder(trackingId);

        //then
        assertThat(response.getOrderStatus()).isEqualTo(orderStatus);
        assertThat(response.getOrderTrackingId()).isEqualTo(trackingId);
        assertThat(response.getFailureMessages()).isEqualTo(failureMessages);
    }

    @DisplayName("Tracking Id에 대힌 주문 정보가 존재하지 않으면 예외가 발생한다.")
    @Test
    void notFoundOrder() {
        //given
        UUID trackingId = UUID.randomUUID();

        //when, then
        assertThatThrownBy(()-> orderTrackingService.trackOrder(trackingId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("trackingId에 대한 주문을 찾을 수 없습니다. trackingId : " + trackingId);
    }
}