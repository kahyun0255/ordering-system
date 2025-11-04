package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.event.orderapproval.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.OrderDetail;
import com.orderingsystem.restaurant.domain.model.OrderProduct;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantInfo;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.ProcessedMessageRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantValidateOrderService;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAcceptService {

    private final ProcessedMessageRepository processedMessageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantValidateOrderService restaurantValidateOrderService;
    private final OrderApprovalRepository orderApprovalRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional
    public void accept(ApprovalRequest approvalRequest) {
        int inserted = processedMessageRepository.insertIgnore(
                approvalRequest.getId(),
                MessageType.ORDER_APPROVAL.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 처리된 Order Approval 메시지입니다. Saga Id : {}, Message Id : {}",
                    approvalRequest.getSagaId(), approvalRequest.getId());
            return;
        }

        log.info("레스토랑 주문 접수 시작. Order Id : {}", approvalRequest.getOrderId());

        List<String> failureMessage = new ArrayList<>();
        Restaurant restaurant = findRestaurant(approvalRequest.getRestaurantId());
        RestaurantInfo restaurantInfo = findRestaurantVO(restaurant.getRestaurantId(), approvalRequest);

        OrderApprovalEvent orderApprovalEvent = restaurantValidateOrderService.validateOrder(restaurantInfo,
                failureMessage, approvalRequest.getSagaId());

        orderAcceptSave(restaurantInfo.getOrderApproval());

        orderOutboxHelper.saveOrderOutboxMessage(
                restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(orderApprovalEvent),
                orderApprovalEvent.getOrderApproval().getStatus(),
                approvalRequest.getSagaId());

        applicationEventPublisher.publishEvent(orderApprovalEvent);
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);

        if (restaurant.isEmpty()) {
            log.error("레스토랑을 찾을 수 없습니다. Restaurant Id : {}", restaurantId);
            throw new RestaurantNotFoundException("레스토랑을 찾을 수 없습니다.");
        }

        return restaurant.get();
    }

    private RestaurantInfo findRestaurantVO(UUID restaurantId, ApprovalRequest approvalRequest) {
        RestaurantInfo restaurant = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .orderDetail(OrderDetail.builder()
                        .orderId(approvalRequest.getOrderId())
                        .orderProducts(approvalRequest.getProducts().stream().map(product ->
                                OrderProduct.builder()
                                        .product(Product.builder()
                                                .productId(product.getProductId())
                                                .build())
                                        .quantity(product.getQuantity())
                                        .build()).toList())
                        .totalAmount(new Money(approvalRequest.getPrice()))
                        .orderStatus(OrderStatus.valueOf(approvalRequest.getRestaurantOrderStatus().name()))
                        .build())
                .build();

        List<UUID> restaurantProductIds = restaurant.getOrderDetail().getOrderProducts().stream()
                .map(op -> op.getProduct().getProductId()).toList();

        List<RestaurantInfoView> restaurantInfos =
                restaurantRepository.findRestaurantInfo(restaurantId, restaurantProductIds);

        if (restaurantInfos.isEmpty()) {
            log.error("레스토랑을 찾을 수 없습니다. Restaurant Id : {}", restaurant.getRestaurantId());
            throw new RestaurantNotFoundException("레스토랑을 찾을 수 없습니다. Restaurant Id : " + restaurant.getRestaurantId());
        }

        restaurant.updateStatus(restaurantInfos.get(0).getRestaurantStaus());

        List<Product> restaurantProducts = restaurantInfos.stream().map(r ->
                Product.builder()
                        .productId(r.getProductId())
                        .name(r.getProductName())
                        .price(r.getProductPrice())
                        .available(r.getProductAvailable())
                        .build()).toList();

        restaurant.getOrderDetail().getOrderProducts().forEach(orderProduct -> {
            restaurantProducts.forEach(p -> {
                if (p.getProductId().equals(orderProduct.getProduct().getProductId())) {
                    orderProduct.getProduct()
                            .updateWithConfirmedNamePriceAndAvailability(p.getName(), p.getPrice(), p.isAvailable());
                }
            });
        });

        return restaurant;
    }

    private void orderAcceptSave(OrderApproval orderApproval) {
        if (!orderApprovalRepository.existsByOrderIdAndRestaurantIdAndStatus(orderApproval.getOrderId(),
                orderApproval.getRestaurantId(), orderApproval.getStatus())) {
            orderApprovalRepository.save(orderApproval);
        }
    }

}
