package com.orderingsystem.order.application.mapper;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.PaymentOrderStatus;
import com.orderingsystem.common.domain.status.RestaurantOrderStatus;
import com.orderingsystem.order.application.dto.request.CreateOrderApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderAddressApplicationRequest;
import com.orderingsystem.order.application.dto.request.OrderItemApplicationRequest;
import com.orderingsystem.order.application.dto.response.CreateOrderResponse;
import com.orderingsystem.order.application.outbox.payment.model.OrderPaymentEventPayload;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventPayload;
import com.orderingsystem.order.application.outbox.restaurant.model.RestaurantApprovalEventProduct;
import com.orderingsystem.order.domain.event.OrderCancelledEvent;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.event.OrderPaidEvent;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderAddress;
import com.orderingsystem.order.domain.model.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderDataMapper {

    public Order createOrderRequestToOrder(CreateOrderApplicationRequest createOrderApplicationRequest,
                                           UUID orderAddress) {
        Order order = Order.builder()
                .customerId(createOrderApplicationRequest.getCustomerId())
                .restaurantId(createOrderApplicationRequest.getRestaurantId())
                .address(orderAddress)
                .price(new Money(createOrderApplicationRequest.getPrice()))
                .build();

        List<OrderItem> items = orderItemsToOrderItemEntity(order, createOrderApplicationRequest.getItems());
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

    public List<UUID> itemsToItemIdList(List<OrderItemApplicationRequest> items) {
        return items.stream().map(OrderItemApplicationRequest::getProductId).toList();
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

    public RestaurantApprovalEventPayload orderPaidEventToRestaurantApprovalEventPayload(
            OrderPaidEvent orderPaidEvent, UUID sagaId) {
        return RestaurantApprovalEventPayload.builder()
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

    public OrderPaymentEventPayload orderCancelledEventToOrderPaymentEventPayload(
            OrderCancelledEvent orderCancelledEvent, UUID sagaId) {
        System.out.println(orderCancelledEvent.getOrder().getFailureMessageList());
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
}
