package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderApprovalStatus;
import com.orderingsystem.common.domain.status.OrderStatus;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_restaurant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant extends AggregateRoot {

    @EmbeddedId
    private RestaurantId restaurantId;

    private String restaurantName;
    private boolean restaurantActive;
    private String productName;
    private BigDecimal productPrice;
    private boolean productAvailable;

    @Transient
    private OrderApproval orderApproval;

    @Transient
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
                .orderApprovalId(UUID.randomUUID())
                .restaurantId(this.getRestaurantId())
                .orderId(this.orderDetail.getOrderId())
                .approvalStatus(orderApprovalStatus)
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
        Restaurant that = (Restaurant) o;
        return Objects.equals(restaurantId, that.restaurantId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(restaurantId);
    }
}
