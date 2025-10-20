package com.orderingsystem.restaurant.application;

import java.util.UUID;

public interface StockCachePort {

    void reserve(UUID productId, int quantity, UUID sagaId);

}
