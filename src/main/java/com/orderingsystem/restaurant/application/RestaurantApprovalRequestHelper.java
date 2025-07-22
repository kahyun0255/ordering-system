package com.orderingsystem.restaurant.application;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import com.orderingsystem.restaurant.application.publisher.OrderApprovedMessagePublisher;
import com.orderingsystem.restaurant.application.publisher.OrderRejectedMessagePublisher;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderDetail;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import com.orderingsystem.restaurant.domain.model.RestaurantInfo;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantValidateOrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantApprovalRequestHelper {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantValidateOrderService restaurantValidateOrderService;
    private final OrderApprovedMessagePublisher orderApprovedMessagePublisher;
    private final OrderRejectedMessagePublisher orderRejectedMessagePublisher;
    private final OrderApprovalRepository orderApprovalRepository;

    @Transactional
    public OrderApprovalEvent persistOrderApproval(ApprovalRequest approvalRequest) {
        log.info("레스토랑 승인 처리 시작. Order Id : {}", approvalRequest.getOrderId());

        List<String> failureMessage = new ArrayList<>();
        Restaurant restaurant = findRestaurant(approvalRequest.getRestaurantId());
        RestaurantInfo restaurantInfo = findRestaurantVO(restaurant.getRestaurantId(), approvalRequest);

        OrderApprovalEvent orderApprovalEvent = restaurantValidateOrderService.validateOrder(restaurantInfo,
                failureMessage, orderApprovedMessagePublisher, orderRejectedMessagePublisher);

        orderApprovalRepository.save(restaurantInfo.getOrderApproval());
        return orderApprovalEvent;
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);

        if (restaurant.isEmpty()) {
            log.error("레스토랑을 찾을 수 없습니다. Restaurant Id : {}", restaurantId);
            throw new RestaurantNotFoundException("레스토랑을 찾을 수 없습니다. Restaurant Id : " + restaurantId);
        }

        return restaurant.get();
    }

    private RestaurantInfo findRestaurantVO(UUID restaurantId, ApprovalRequest approvalRequest) {
        RestaurantInfo restaurant = RestaurantInfo.builder()
                .restaurantId(restaurantId)
                .orderDetail(OrderDetail.builder()
                        .orderId(approvalRequest.getOrderId())
                        .products(approvalRequest.getProducts().stream().map(product ->
                                Product.builder()
                                        .productId(product.getProductId())
                                        .quantity(product.getQuantity())
                                        .build()).toList())
                        .totalAmount(new Money(approvalRequest.getPrice()))
                        .orderStatus(OrderStatus.valueOf(approvalRequest.getRestaurantOrderStatus().name()))
                        .build())
                .build();

        List<UUID> restaurantProductIds = restaurant.getOrderDetail().getProducts().stream()
                .map(Product::getProductId).toList();

        Optional<List<RestaurantInfoView>> restaurantInfos =
                restaurantRepository.findRestaurantProducts(restaurantId, restaurantProductIds);

        if (restaurantInfos.isEmpty()) {
            log.error("레스토랑을 찾을 수 없습니다. Restaurant Id : {}", restaurant.getRestaurantId());
            throw new RestaurantNotFoundException("레스토랑을 찾을 수 없습니다. Restaurant Id : " + restaurant.getRestaurantId());
        }

        restaurant.updateActive(restaurantInfos.get().get(0).getRestaurantActive());

        List<Product> restaurantProducts = restaurantInfos.get().stream().map(r ->
                Product.builder()
                        .productId(r.getProductId())
                        .name(r.getProductName())
                        .price(r.getProductPrice())
                        .available(r.getProductAvailable())
                        .build()).toList();

        restaurant.getOrderDetail().getProducts().forEach(product -> {
            restaurantProducts.forEach(p -> {
                if (p.getProductId().equals(product.getProductId())) {
                    product.updateWithConfirmedNamePriceAndAvailability(p.getName(), p.getPrice(), p.isAvailable());
                }
            });
        });

        return restaurant;
    }
}
