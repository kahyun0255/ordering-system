package com.orderingsystem.order.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends AggregateRoot {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    @Column(columnDefinition = "varchar(36)")
    private UUID customerId;

    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;

    @Column(columnDefinition = "varchar(36)")
    private UUID trackingId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price"))
    private Money price;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(columnDefinition = "TEXT")
    private String failureMessages;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private OrderAddress address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order that = (Order) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static final String FAILURE_MESSAGE_DELIMITER = ", ";

    public void initializeOrder() {
        id = UUID.randomUUID();
        trackingId = UUID.randomUUID();
        orderStatus = OrderStatus.PENDING;
        initializeOrderItems();
    }

    private void initializeOrderItems() {
        long itemId = 1;
        for (OrderItem orderItem : items) {
            orderItem.initializeOrderItem(this, itemId++);
        }
    }

    public void validateOrder() {
        validateInitialOrder();
        validateTotalPrice();
        validateItemsPrice();
    }

    private void validateInitialOrder() {
        if (orderStatus != null || getId() != null) {
            throw new OrderDomainException("주문이 초기화될 수 없는 상태입니다.(이미 생성된 주문일 수 있습니다.)");
        }
    }

    private void validateTotalPrice() {
        if (price == null || !price.isGreaterThanZero()) {
            throw new OrderDomainException("총 주문 금액은 0보다 커야합니다.");
        }
    }

    private void validateItemsPrice() {
        Money orderItemsTotal = items.stream().map(orderItem -> {
            validateItemPrice(orderItem);
            return orderItem.getSubTotal();
        }).reduce(Money.ZERO, Money::add);

        if (!price.equals(orderItemsTotal)) {
            throw new OrderDomainException("총 주문 금액 : " + price.getAmount()
                    + " 개별 항목들의 합계 : " + orderItemsTotal.getAmount() + " 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다.");
        }
    }

    private void validateItemPrice(OrderItem orderItem) {
        if (!orderItem.isPriceValid()) {
            throw new OrderDomainException("상품: " + orderItem.getProduct().getProductId() +
                    "의 항목 가격 : " + orderItem.getProduct().getPrice().getAmount() + "이 유효하지 않습니다.");
        }
    }

    public void pay() {
        if (orderStatus != OrderStatus.PENDING) {
            throw new OrderDomainException("결제를 진행할 수 없는 주문 상태입니다.");
        }
        orderStatus = OrderStatus.PAID;
    }

    public void approve() {
        if (orderStatus != OrderStatus.PAID) {
            throw new OrderDomainException("승인할 수 없는 주문 상태입니다.");
        }
        orderStatus = OrderStatus.APPROVED;
    }

    public void initCancel(List<String> failureMessages) {
        if (orderStatus != OrderStatus.PAID) {
            throw new OrderDomainException("주문을 취소할 수 없는 상태입니다.");
        }
        orderStatus = OrderStatus.CANCELLING;
        updateFailureMessages(failureMessages);
    }

    public void cancel(List<String> failureMessages) {
        if (!(orderStatus == OrderStatus.CANCELLING || orderStatus == OrderStatus.PENDING)) {
            throw new OrderDomainException("주문을 취소 완료할 수 없는 상태입니다.");
        }
        orderStatus = OrderStatus.CANCELLED;
        updateFailureMessages(failureMessages);
    }

    public List<String> getFailureMessageList() {
        if (failureMessages == null || failureMessages.isBlank()) {
            return List.of();
        }
        return List.of(failureMessages.split(","));
    }

    public void updateFailureMessages(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            this.failureMessages = null;
        } else {
            this.failureMessages = String.join(",", messages);
            System.out.println(this.failureMessages);
        }
    }

}
