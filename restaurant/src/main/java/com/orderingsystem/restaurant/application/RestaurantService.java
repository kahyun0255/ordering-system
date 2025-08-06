package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.ORDER_SAGA_NAME;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.domain.status.OrderStatus;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.ApprovalRequest;
import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.event.OrderApprovalEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.OrderDetail;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantInfo;
import com.orderingsystem.restaurant.domain.model.RestaurantInfoView;
import com.orderingsystem.restaurant.domain.model.outbox.OrderOutbox;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.OrderOutboxRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantValidateOrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantValidateOrderService restaurantValidateOrderService;
    private final OrderApprovalRepository orderApprovalRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional
    public void approveOrder(ApprovalRequest approvalRequest) {
        if (isOutboxMessageProcessedForApproval(approvalRequest)) {
            log.info("해당 Saga Id : {} 에 대한 Outbox 메시지가 이미 처리 완료 상태로 저장되어있어 메시지를 다시 처리하지 않습니다.",
                    approvalRequest.getSagaId());
            return;
        }

        log.info("레스토랑 승인 처리 시작. Order Id : {}", approvalRequest.getOrderId());

        List<String> failureMessage = new ArrayList<>();
        Restaurant restaurant = findRestaurant(approvalRequest.getRestaurantId());
        RestaurantInfo restaurantInfo = findRestaurantVO(restaurant.getRestaurantId(), approvalRequest);

        OrderApprovalEvent orderApprovalEvent =
                restaurantValidateOrderService.validateOrder(restaurantInfo, failureMessage);

        orderApprovalSave(restaurantInfo.getOrderApproval());

        orderOutboxHelper.saveOrderOutboxMessage(
                restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(orderApprovalEvent),
                orderApprovalEvent.getOrderApproval().getStatus(),
                OutboxStatus.STARTED,
                approvalRequest.getSagaId());
    }

    private boolean isOutboxMessageProcessedForApproval(ApprovalRequest approvalRequest) {
        List<OrderOutbox> orderOutboxMessage = orderOutboxRepository.findByTypeAndSagaIdAndOutboxStatus(
                ORDER_SAGA_NAME, approvalRequest.getSagaId(), OutboxStatus.COMPLETED);

        return !orderOutboxMessage.isEmpty();
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

        List<RestaurantInfoView> restaurantInfos =
                restaurantRepository.findRestaurantInfo(restaurantId, restaurantProductIds);

        if (restaurantInfos.isEmpty()) {
            log.error("레스토랑을 찾을 수 없습니다. Restaurant Id : {}", restaurant.getRestaurantId());
            throw new RestaurantNotFoundException("레스토랑을 찾을 수 없습니다. Restaurant Id : " + restaurant.getRestaurantId());
        }

        restaurant.updateActive(restaurantInfos.get(0).getRestaurantActive());

        List<Product> restaurantProducts = restaurantInfos.stream().map(r ->
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

    private void orderApprovalSave(OrderApproval orderApproval) {
        if (!orderApprovalRepository.existsByOrderIdAndRestaurantIdAndStatus(orderApproval.getOrderId(),
                orderApproval.getRestaurantId(), orderApproval.getStatus())){
            orderApprovalRepository.save(orderApproval);
        }
    }
}
