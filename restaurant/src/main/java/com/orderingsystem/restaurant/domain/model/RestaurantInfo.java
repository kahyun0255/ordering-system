package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovedEvent;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderRejectedEvent;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RestaurantInfo {

    private UUID restaurantId;
    private String restaurantName;
    private RestaurantStatus status;
    @Setter
    private OrderApproval orderApproval;
    @Setter
    private OrderDetail orderDetail;

    public void validateOrder(List<String> failureMessages) {
        if (orderDetail.getOrderStatus() != OrderStatus.PAID) {
            log.error("해당 주문은 결제가 완료되지 않았습니다. Order Id : {}", orderDetail.getOrderId());
            failureMessages.add("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderDetail.getOrderId());
        }

        Money totalAmount = orderDetail.getOrderProducts().stream().map(op -> {
            if (!op.getProduct().isAvailable()) {
                log.error("상품 Id가 {} 인 상품은 주문이 불가능한 상태입니다. Order Id : {}",
                        op.getProduct().getProductId(), orderDetail.getOrderId());
                failureMessages.add("상품 Id가 " + op.getProduct().getProductId() + "인 상품은 주문이 불가능한 상태입니다.");
            }
            return op.getProduct().getPrice().multiply(op.getQuantity());
        }).reduce(Money.ZERO, Money::add);

        if (!totalAmount.equals(orderDetail.getTotalAmount())) {
            log.error("해당 주문의 총 금액이 올바르지 않습니다. Order Id : {}", orderDetail.getOrderId());
            failureMessages.add("해당 주문의 총 금액이 올바르지 않습니다. Order Id : " + orderDetail.getOrderId());
        }

        if (!status.equals(RestaurantStatus.ACTIVE)) {
            log.error("레스토랑이 주문을 받을 수 없는 상태입니다. Order Id : {}", orderDetail.getOrderId());
            failureMessages.add("레스토랑이 주문을 받을 수 없는 상태입니다. Order Id : " + orderDetail.getOrderId());
        }
    }

    public void updateStatus(RestaurantStatus status) {
        this.status = status;
    }

    public OrderApprovedEvent approveOrder(List<String> failureMessages, UUID sagaId) {
        this.orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .restaurantId(this.getRestaurantId())
                .orderId(this.orderDetail.getOrderId())
                .status(OrderApprovalStatus.APPROVED)
                .build();

        log.info("주문이 승인되었습니다. Order Id : {}", this.getOrderDetail().getOrderId());

        return new OrderApprovedEvent(this.getOrderApproval(), this.getRestaurantId(), sagaId, failureMessages,
                ZonedDateTime.now());
    }

    public OrderRejectedEvent rejectOrder(List<String> failureMessages, UUID sagaId) {
        this.orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .restaurantId(this.getRestaurantId())
                .orderId(this.orderDetail.getOrderId())
                .status(OrderApprovalStatus.REJECTED)
                .build();

        log.info("주문이 거절되었습니다. Order Id : {}", this.getOrderDetail().getOrderId());

        return new OrderRejectedEvent(this.getOrderApproval(), this.getRestaurantId(), sagaId, failureMessages,
                ZonedDateTime.now());
    }

}
