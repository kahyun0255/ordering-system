package com.orderingsystem.order.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.event.OrderRejectedEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class Order extends AggregateRoot {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;

    @Column(columnDefinition = "varchar(36)")
    private UUID customerId;

    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;

    @Column(columnDefinition = "varchar(36)", unique = true)
    private UUID trackingId;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price"))
    private Money price;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(columnDefinition = "TEXT")
    private String failureMessages;

    private UUID address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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
        for (OrderItem orderItem : items) {
            orderItem.initializeOrderItem(this);
        }
    }

    public void validateOrder(List<String> failureMessages) {
        validateInitialOrder();
        validateTotalPrice(failureMessages);
        if (!failureMessages.isEmpty()) {
            return;
        }
        validateItems(failureMessages);
    }

    private void validateInitialOrder() {
        if (orderStatus != null || getId() != null) {
            throw new OrderDomainException("주문이 초기화될 수 없는 상태입니다.(이미 생성된 주문일 수 있습니다.)");
        }
    }

    private void validateTotalPrice(List<String> failureMessages) {
        if (price == null || !price.isGreaterThanZero()) {
            log.warn("총 주문 금액은 0보다 커야합니다. Order Id : {}", this.id);
            failureMessages.add("총 주문 금액은 0보다 커야합니다.");
        }
    }

    private void validateItems(List<String> failureMessages) {
        Money orderItemsTotal = items.stream().map(orderItem -> {
            validateItemPrice(orderItem, failureMessages);
            return orderItem.getSubTotal();
        }).reduce(Money.ZERO, Money::add);

        if (!price.equals(orderItemsTotal)) {
            log.warn("총 주문 금액 : {} 개별 항목들의 합계 : {}. 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다. Order Id : {}", price.getAmount(),
                    orderItemsTotal.getAmount(), this.id);
            failureMessages.add("총 주문 금액 : " + price.getAmount()
                    + " 개별 항목들의 합계 : " + orderItemsTotal.getAmount() + ". 총 주문 금액과 개별 항목들의 합계가 일치하지 않습니다.");
        }
    }

    private void validateItemPrice(OrderItem orderItem, List<String> failureMessages) {
        if (!orderItem.isPriceValid()) {
            log.warn("상품 : {}의 항목 가격 : {}이 유효하지 않습니다. Order Id : {}", orderItem.getProductId(),
                    orderItem.getPrice().getAmount(), this.id);
            failureMessages.add("상품 : " + orderItem.getProductId() +
                    "의 항목 가격 : " + orderItem.getPrice().getAmount() + "이 유효하지 않습니다.");
        }
    }

    public OrderPaidEvent pay() {
        if (orderStatus != OrderStatus.PENDING) {
            throw new OrderDomainException("결제를 진행할 수 없는 주문 상태입니다.");
        }
        orderStatus = OrderStatus.PAID;

        return new OrderPaidEvent(this, ZonedDateTime.now());
    }

    public void accept() {
        if (orderStatus != OrderStatus.PAID) {
            throw new OrderDomainException("접수할 수 없는 주문 상태입니다.");
        }
        orderStatus = OrderStatus.ACCEPTED;
    }

    public void approve() {
        if (orderStatus != OrderStatus.ACCEPTED) {
            throw new OrderDomainException("승인할 수 없는 주문 상태입니다.");
        }
        orderStatus = OrderStatus.APPROVED;
    }

    public OrderRejectedEvent rejecting() {
        orderStatus = OrderStatus.REJECTING;
        return new OrderRejectedEvent(this, ZonedDateTime.now());
    }

    public OrderCancelledEvent initCancel(List<String> failureMessages) {
        if (orderStatus != OrderStatus.PAID) {
            throw new OrderDomainException("주문을 취소할 수 없는 상태입니다.");
        }
        orderStatus = OrderStatus.CANCELLING;
        updateFailureMessages(failureMessages);

        return new OrderCancelledEvent(this, ZonedDateTime.now());
    }

    public OrderCancelledEvent requestCancelByCustomer(List<String> failureMessages) {
        if (orderStatus != OrderStatus.PENDING && orderStatus != OrderStatus.PAID
                && orderStatus != OrderStatus.ACCEPTED) {
            throw new OrderDomainException("주문을 취소할 수 없는 상태입니다.");
        }
        orderStatus = OrderStatus.CANCELLING;
        updateFailureMessages(failureMessages);

        return new OrderCancelledEvent(this, ZonedDateTime.now());
    }

    public void cancel(List<String> failureMessages) {
        if (!(orderStatus == OrderStatus.CANCELLING || orderStatus == OrderStatus.PENDING)) {
            throw new OrderDomainException("주문을 취소 완료할 수 없는 상태입니다.");
        }
        orderStatus = OrderStatus.CANCELLED;
        updateFailureMessages(failureMessages);
    }

    public void reject() {
        orderStatus = OrderStatus.REJECTED;
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
        }
    }

    public void updateItems(List<OrderItem> items) {
        this.items = items;
    }
}
