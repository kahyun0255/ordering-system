package com.orderingsystem.restaurant.application;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockFacade {

    private final StockCachePort stockCachePort;
    private final ProductStockService productStockService;

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
        productStockService.confirm(history);
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

        productStockService.restore(confirmed);
        stockCachePort.restoreConfirmed(confirmed, orderId);
    }
}
