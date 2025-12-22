package com.orderingsystem.order.application.mapper;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import com.orderingsystem.order.application.dto.request.ValidationCouponApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.outbox.coupon.model.OrderCouponEventPayload;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.application.outbox.product.model.OrderProductEventPayload;
import com.orderingsystem.order.application.outbox.product.model.OrderProductEventProduct;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantAcceptEventPayload;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventProduct;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.event.OrderRejectedEvent;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderAddress;
import com.orderingsystem.order.domain.model.OrderItem;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderDataMapper {

    public Order createOrderRequestToOrder(CreateOrderApplicationRequest request,
                                           UUID orderAddress) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .restaurantId(request.getRestaurantId())
                .address(orderAddress)
                .price(new Money(request.getPrice()))
                .couponIds(request.getCouponId())
                .build();

        List<OrderItem> items = orderItemsToOrderItemEntity(order, request.getItems());
        order.updateItems(items);

        return order;
    }

    public OrderAddress orderAddressToStreetAddress(OrderAddressApplicationRequest address) {
        return OrderAddress.builder()
                .id(UUID.randomUUID())
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .build();
    }

    private List<OrderItem> orderItemsToOrderItemEntity(Order order, List<OrderItemApplicationRequest> items) {
        return items.stream().map(orderItem ->
                        OrderItem.builder()
                                .order(order)
                                .price(new Money(orderItem.getPrice()))
                                .quantity(orderItem.getQuantity())
                                .subTotal(new Money(orderItem.getSubTotal()))
                                .productId(orderItem.getProductId())
                                .build())
                .toList();
    }

    public CreateOrderResponse orderToCreateOrderResponse(Order order, String massage) {
        return CreateOrderResponse.builder()
                .orderTrackingId(order.getTrackingId())
                .orderStatus(order.getOrderStatus())
                .message(massage)
                .build();
    }

    public OrderPaymentEventPayload orderCreatedToOrderPaymentEventPayload(OrderCreateEvent orderCreateEvent,
                                                                           UUID sagaId) {
        return OrderPaymentEventPayload.builder()
                .customerId(orderCreateEvent.getOrder().getCustomerId().toString())
                .orderId(orderCreateEvent.getOrder().getId().toString())
                .sagaId(sagaId.toString())
                .price(orderCreateEvent.getOrder().getPrice().getAmount())
                .createdAt(orderCreateEvent.getCreatedAt())
                .paymentOrderStatus(PaymentOrderStatus.PENDING.name())
                .build();
    }

    public RestaurantAcceptEventPayload orderPaidEventToRestaurantAcceptEventPayload(
            OrderPaidEvent orderPaidEvent, UUID sagaId) {
        return RestaurantAcceptEventPayload.builder()
                .orderId(orderPaidEvent.getOrder().getId().toString())
                .restaurantId(orderPaidEvent.getOrder().getRestaurantId().toString())
                .sagaId(sagaId.toString())
                .restaurantOrderStatus(RestaurantOrderStatus.PAID.name())
                .products(orderPaidEvent.getOrder().getItems().stream().map(orderItem ->
                        RestaurantApprovalEventProduct.builder()
                                .id(orderItem.getProductId().toString())
                                .quantity(orderItem.getQuantity())
                                .build()).toList())
                .price(orderPaidEvent.getOrder().getPrice().getAmount())
                .createdAt(orderPaidEvent.getCreatedAt())
                .build();
    }

    public RestaurantAcceptEventPayload orderToRestaurantAcceptEventPayload(
            Order order, UUID sagaId) {
        return RestaurantAcceptEventPayload.builder()
                .orderId(order.getId().toString())
                .restaurantId(order.getRestaurantId().toString())
                .sagaId(sagaId.toString())
                .restaurantOrderStatus(RestaurantOrderStatus.PAID.name())
                .products(order.getItems().stream().map(orderItem ->
                        RestaurantApprovalEventProduct.builder()
                                .id(orderItem.getProductId().toString())
                                .quantity(orderItem.getQuantity())
                                .build()).toList())
                .price(order.getPrice().getAmount())
                .createdAt(ZonedDateTime.now())
                .build();
    }

    public OrderPaymentEventPayload orderCancelledEventToOrderPaymentEventPayload(
            OrderCancelledEvent orderCancelledEvent, UUID sagaId) {
        return OrderPaymentEventPayload.builder()
                .orderId(orderCancelledEvent.getOrder().getId().toString())
                .sagaId(sagaId.toString())
                .customerId(orderCancelledEvent.getOrder().getCustomerId().toString())
                .price(orderCancelledEvent.getOrder().getPrice().getAmount())
                .createdAt(orderCancelledEvent.getCreatedAt())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED.name())
                .failureMessage(orderCancelledEvent.getOrder().getFailureMessageList())
                .build();
    }

    public OrderProductEventPayload orderToStockReservationCancelEventPayload(Order order, UUID sagaId, String type) {
        return OrderProductEventPayload.builder()
                .sagaId(sagaId.toString())
                .orderId(order.getId().toString())
                .restaurantId(order.getRestaurantId().toString())
                .type(type)
                .createdAt(ZonedDateTime.now())
                .products(order.getItems().stream().map(item -> {
                    return OrderProductEventProduct.builder()
                            .id(item.getProductId().toString())
                            .quantity(item.getQuantity())
                            .build();
                }).toList())
                .build();
    }

    public OrderPaymentEventPayload orderRejectedEventToOrderPaymentEventPayload(OrderRejectedEvent orderRejectedEvent,
                                                                                 UUID sagaId) {
        return OrderPaymentEventPayload.builder()
                .orderId(orderRejectedEvent.getOrder().getId().toString())
                .sagaId(sagaId.toString())
                .customerId(orderRejectedEvent.getOrder().getCustomerId().toString())
                .price(orderRejectedEvent.getOrder().getPrice().getAmount())
                .createdAt(orderRejectedEvent.getCreatedAt())
                .paymentOrderStatus(PaymentOrderStatus.CANCELLED.name())
                .failureMessage(orderRejectedEvent.getOrder().getFailureMessageList())
                .build();
    }

    public ValidationCouponApplicationRequest createOrderRequestToValidationCouponApplicationRequest(
            CreateOrderApplicationRequest createOrderRequest) {
        return ValidationCouponApplicationRequest.builder()
                .customerId(createOrderRequest.getCustomerId())
                .couponIds(createOrderRequest.getCouponId())
                .totalOrderAmount(createOrderRequest.getPrice())
                .build();
    }

    public OrderCouponEventPayload orderCreatedToOrderCouponEventPayload(OrderCreateEvent orderCreateEvent,
                                                                         UUID sagaId, List<Long> couponId) {
        List<String> couponIdString = couponId.stream().map(Object::toString).toList();

        return OrderCouponEventPayload.builder()
                .orderId(orderCreateEvent.getOrder().getId().toString())
                .customerId(orderCreateEvent.getOrder().getCustomerId().toString())
                .sagaId(sagaId.toString())
                .issuedCouponId(couponIdString)
                .createdAt(orderCreateEvent.getCreatedAt())
                .failureMessage(orderCreateEvent.getOrder().getFailureMessageList())
                .build();
    }

}
