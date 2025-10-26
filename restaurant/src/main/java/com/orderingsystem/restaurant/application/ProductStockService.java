package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockService {

    private final ProductRepository productRepository;

    @Transactional
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

        for (Product product : products){
            int quantity = productMap.get(product.getProductId());
            product.decreaseStock(quantity);
        }
    }

}
