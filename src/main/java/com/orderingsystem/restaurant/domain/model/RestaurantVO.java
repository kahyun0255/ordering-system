package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantVO extends AggregateRoot {

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
            failureMessages.add("Payment is not completed for order : " + orderDetail.getOrderId());
        }

        Money totalAmount = orderDetail.getProducts().stream().map(product -> {
            if (!product.isAvailable()) {
                failureMessages.add("Product with id : " + product.getProductId() + "is not available");
            }
            return product.getPrice().multiply(product.getQuantity());
        }).reduce(Money.ZERO, Money::add);

        if (!totalAmount.equals(orderDetail.getTotalAmount())) {
            failureMessages.add("Price total is not correct for order : " + orderDetail.getOrderId());
        }
    }

    public void constructOrderApproval(OrderApprovalStatus orderApprovalStatus) {
        this.orderApproval=OrderApproval.builder()
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
        RestaurantVO that = (RestaurantVO) o;
        return Objects.equals(restaurantId, that.restaurantId) && Objects.equals(productId,
                that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restaurantId, productId);
    }
}
