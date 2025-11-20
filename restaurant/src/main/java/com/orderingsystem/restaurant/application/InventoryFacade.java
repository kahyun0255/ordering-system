package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.ProductRequest;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryFacade {

    private final StockCachePort stockCachePort;
    private final InventoryService inventoryService;
    private final OrderCancelService orderCancelService;

    public void reserve(UUID productId, int quantity, UUID sagaId) {
        stockCachePort.reserve(productId, quantity, sagaId);
    }

    public void confirm(UUID sagaId, UUID orderId) {
        Map<Object, Object> history = stockCachePort.getHistory(sagaId);
        if (history == null || history.isEmpty()) {
            log.warn("재고 예약 내역이 존재하지 않습니다. sagaId = {}", sagaId);
            return;
        }

        stockCachePort.confirm(history, sagaId, orderId);
        inventoryService.confirm(history);
    }

    public void cancelByState(ProductRequest productRequest) {
        log.info("주문 취소로 인한 재고 복구 진행. orderId : [{}]", productRequest.getOrderId());
        if (!orderCancelService.checkAndMarkProcessed(productRequest, MessageType.INVENTORY_COMPENSATE)) {
            return;
        }

        UUID orderId = productRequest.getOrderId();
        UUID sagaId = productRequest.getSagaId();

        Map<Object, Object> confirmed = stockCachePort.getConfirmed(orderId);
        if (confirmed != null && !confirmed.isEmpty()) {
            inventoryService.cancel(confirmed, orderId, sagaId);
            stockCachePort.restoreConfirmed(confirmed, orderId);
        }

        Map<Object, Object> history = stockCachePort.getHistory(sagaId);
        if (history!=null &&!history.isEmpty()){
            stockCachePort.cancelReservation(history, sagaId);
            inventoryService.cancelReservation(orderId);
        }

        log.warn("취소할 Redis 상태가 없습니다. orderId={}, sagaId={}", orderId, sagaId);
    }

    public void cancelReservation(UUID sagaId) {
        Map<Object, Object> history = stockCachePort.getHistory(sagaId);
        if (history == null) {
            return;
        }

        stockCachePort.cancelReservation(history, sagaId);
    }

    public void cancel(UUID orderId) {
        Map<Object, Object> confirmed = stockCachePort.getConfirmed(orderId);
        if (confirmed == null || confirmed.isEmpty()) {
            log.warn("복구할 재고 내역이 없습니다. sagaId = {}", orderId);
            return;
        }

        inventoryService.restore(confirmed);
        stockCachePort.restoreConfirmed(confirmed, orderId);
    }
}
