package com.orderingsystem.restaurant.application;

import java.util.Map;
import java.util.UUID;

public interface StockCachePort {

    void reserve(UUID productId, int quantity, UUID sagaId);

    Map<Object, Object> getHistory(UUID sagaId);

    void confirm(Map<Object, Object> history, UUID sagaId, UUID orderId);

    void cancelReservation(Map<Object, Object> history, UUID sagaId);

    Map<Object, Object> getConfirmed(UUID orderId);

    void restoreConfirmed(Map<Object, Object> confirmed, UUID orderId);

    void update(UUID productId, int quantity);

    void delete(UUID productId);
}
