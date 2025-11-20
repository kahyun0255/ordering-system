package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.mapper.RestaurantDataMapper;
import com.orderingsystem.restaurant.application.outbox.order.OrderOutboxHelper;
import com.orderingsystem.restaurant.domain.event.order.orderapproval.OrderCancelledEvent;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.OrderApproval;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.OrderApprovalRepository;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockService {

    private final ProductRepository productRepository;
    private final OrderApprovalRepository orderApprovalRepository;
    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantDataMapper restaurantDataMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(Map<Object, Object> history) {
        Map<UUID, Integer> productMap = new HashMap<>();
        List<UUID> productIds = new ArrayList<>();

        history.forEach((productIdStr, quantityStr) -> {
            UUID productId = UUID.fromString(productIdStr.toString());
            int quantity = Integer.parseInt(quantityStr.toString());
            productMap.put(productId, quantity);
            productIds.add(productId);
        });

        List<Product> products = productRepository.findAllById(productIds);

        for (Product product : products) {
            int quantity = productMap.get(product.getProductId());
            product.decreaseStock(quantity);
        }
    }

    @Transactional
    public void cancel(Map<Object, Object> confirmed, UUID orderId, UUID sagaId) {
        log.info("주문 취소 시작. Order Id : [{}]", orderId);

        restore(confirmed);
        OrderApproval orderApproval = orderApprovalRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RestaurantNotFoundException("주문 정보를 찾을 수 없습니다."));

        OrderCancelledEvent orderCancelledEvent = orderApproval.cancel();

        orderOutboxHelper.saveOrderOutboxMessage(
                restaurantDataMapper.restaurantApprovalEventToOrderEventPayload(orderCancelledEvent),
                orderCancelledEvent.getOrderApproval().getStatus(),
                sagaId);
    }

    @Transactional
    public void restore(Map<Object, Object> confirmed) {
        if (confirmed == null || confirmed.isEmpty()) {
            return;
        }

        Map<UUID, Integer> map = new HashMap<>();
        List<UUID> ids = new ArrayList<>();

        confirmed.forEach((pid, qty) -> {
            UUID id = UUID.fromString(pid.toString());
            int q = Integer.parseInt(qty.toString());
            map.put(id, q);
            ids.add(id);
        });

        List<Product> products = productRepository.findAllById(ids);
        for (Product p : products) {
            p.increaseStock(map.get(p.getProductId()));
        }
    }

    @Transactional
    public void cancelReservation(UUID orderId) {
        OrderApproval orderApproval = orderApprovalRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RestaurantNotFoundException("주문 정보를 찾을 수 없습니다."));

        orderApproval.cancel();
    }

}
