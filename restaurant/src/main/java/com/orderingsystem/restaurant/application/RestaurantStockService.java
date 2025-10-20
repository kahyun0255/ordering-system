package com.orderingsystem.restaurant.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantStockService {

    private final StockCachePort redisStock;

    public void reserve(UUID productId, int quantity, UUID sagaId) {
        redisStock.reserve(productId, quantity, sagaId);
    }

}
