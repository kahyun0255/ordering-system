package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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
public class RestaurantInfo extends AggregateRoot {

    private UUID restaurantId;
    private UUID productId;
    private String restaurantName;
    private Boolean restaurantActive;
    private String productName;
    private BigDecimal productPrice;
    private Boolean productAvailable;
    @Setter
    private OrderApproval orderApproval;
    @Setter
    private OrderDetail orderDetail;

    public void validateOrder(List<String> failureMessages) {
        if (orderDetail.getOrderStatus() != OrderStatus.PAID) {
            log.error("해당 주문은 결제가 완료되지 않았습니다. Order Id : {}", orderDetail.getOrderId());
            failureMessages.add("해당 주문은 결제가 완료되지 않았습니다. Order Id : " + orderDetail.getOrderId());
        }

        Money totalAmount = orderDetail.getProducts().stream().map(product -> {
            if (!product.isAvailable()) {
                log.error("상품 Id가 {} 인 상품은 주문이 불가능한 상태입니다. Order Id : {}",
                        product.getProductId(), orderDetail.getOrderId());
                failureMessages.add("상품 Id가 " + product.getProductId() + "인 상품은 주문이 불가능한 상태입니다.");
            }
            return product.getPrice().multiply(product.getQuantity());
        }).reduce(Money.ZERO, Money::add);

        if (!totalAmount.equals(orderDetail.getTotalAmount())) {
            log.error("해당 주문의 총 금액이 올바르지 않습니다. Order Id : {}", orderDetail.getOrderId());
            failureMessages.add("해당 주문의 총 금액이 올바르지 않습니다. Order Id : " + orderDetail.getOrderId());
        }
    }

    public void constructOrderApproval(OrderApprovalStatus orderApprovalStatus) {
        this.orderApproval = OrderApproval.builder()
                .id(UUID.randomUUID())
                .restaurantId(this.getRestaurantId())
                .orderId(this.orderDetail.getOrderId())
                .status(orderApprovalStatus)
                .build();
    }

    public void updateActive(boolean active) {
        this.restaurantActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestaurantInfo that = (RestaurantInfo) o;
        return Objects.equals(restaurantId, that.restaurantId) && Objects.equals(productId,
                that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restaurantId, productId);
    }
}
