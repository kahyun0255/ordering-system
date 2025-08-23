package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest;
import com.orderingsystem.restaurant.application.dto.request.RestaurantValidationApplicationRequest.Item;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantValidationService {

    private final RestaurantProductRepository restaurantProductRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public void validate(Restaurant restaurant, RestaurantValidationApplicationRequest restaurantValidationRequest) {
        if (restaurantValidationRequest.getItems() == null || restaurantValidationRequest.getItems().isEmpty()) {
            log.warn("주문 상품이 비어있습니다. Saga Id : {}", restaurantValidationRequest.getSagaId());
            throw new IllegalArgumentException("주문 상품이 비어있습니다.");
        }

        Map<UUID, RestaurantValidationApplicationRequest.Item> itemMap = mergeDuplicateItems(
                restaurantValidationRequest.getItems());

        List<UUID> productIds = new ArrayList<>(itemMap.keySet());
        List<Product> products = productRepository.findAllById(productIds);

        validateRestaurant(restaurant, restaurantValidationRequest.getSagaId());
        validateQuantities(restaurantValidationRequest, restaurantValidationRequest.getSagaId());
        validateProductsExist(restaurantValidationRequest.getSagaId(), products, productIds);
        validateProductAvailabilityAndPrice(restaurantValidationRequest, products, itemMap);
        validateRestaurantSellsProducts(restaurant, restaurantValidationRequest, productIds);
    }

    private void validateRestaurant(Restaurant restaurant, UUID sagaId) {
        if (!restaurant.getStatus().equals(RestaurantStatus.ACTIVE)) {
            log.warn("레스토랑이 주문 가능한 상태가 아닙니다. Restaurant Id : {}, Saga Id : {}", restaurant.getRestaurantId(), sagaId);
            throw new IllegalArgumentException("레스토랑이 주문 가능한 상태가 아닙니다.");
        }
    }

    private void validateQuantities(RestaurantValidationApplicationRequest restaurantValidationRequest, UUID sagaId) {
        restaurantValidationRequest.getItems().forEach(item -> {
            if (item.getQuantity() <= 0) {
                log.warn("상품 수량이 유효하지 않습니다. Product Id : {}, Quantity : {}, Saga Id : {}", item.getProductId(),
                        item.getQuantity(), sagaId);
                throw new IllegalArgumentException("상품 수량이 유효하지 않습니다.");
            }
        });
    }

    private void validateProductsExist(UUID sagaId, List<Product> products, List<UUID> productIds) {
        Map<UUID, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        Set<UUID> missing = productIds.stream()
                .filter(id -> !productById.containsKey(id))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            log.warn("주문에 존재하지 않는 상품이 포함되어있습니다. Product Id : {}, Saga Id : {}",
                    missing, sagaId);
            throw new IllegalArgumentException("주문에 존재하지 않는 상품이 포함되어있습니다.");
        }
    }

    private void validateProductAvailabilityAndPrice(RestaurantValidationApplicationRequest restaurantValidationRequest,
                                                     List<Product> products, Map<UUID, Item> itemMap) {
        BigDecimal expectedTotal = BigDecimal.ZERO;

        for (Product product : products) {
            if (!product.isAvailable()) {
                log.warn("주문에 판매 불가능한 상품이 포함되어 있습니다. Product Id : {}, Saga Id : {}", product.getProductId(),
                        restaurantValidationRequest.getSagaId());
                throw new IllegalArgumentException("주문에 판매 불가능한 상품이 포함되어 있습니다.");
            }

            Item item = itemMap.get(product.getProductId());
            if (product.getPrice().getAmount().compareTo(item.getPrice()) != 0) {
                log.warn("주문을 요청한 상품의 가격과 판매 가격이 일치하지 않습니다. Product Id : {}, 주문 가격 : {}, 판매 가격 : {}, Saga Id : {}",
                        product.getProductId(), item.getPrice(), product.getPrice().getAmount(),
                        restaurantValidationRequest.getSagaId());
                throw new IllegalArgumentException("주문을 요청한 상품의 가격과 판매 가격이 일치하지 않습니다.");
            }

            BigDecimal lineTotal = product.getPrice().multiply(item.getQuantity()).getAmount();
            expectedTotal = expectedTotal.add(lineTotal);
        }

        validateTotalPrice(restaurantValidationRequest, expectedTotal);
    }

    private void validateTotalPrice(RestaurantValidationApplicationRequest restaurantValidationRequest,
                                    BigDecimal expectedTotal) {
        if (restaurantValidationRequest.getTotalPrice() == null
                || expectedTotal.compareTo(restaurantValidationRequest.getTotalPrice()) != 0) {
            log.warn("총 주문 금액이 일치하지 않습니다. 요청 합계 : {}, 계산 합계 : {}, Saga Id : {}",
                    restaurantValidationRequest.getTotalPrice(), expectedTotal,
                    restaurantValidationRequest.getSagaId());
            throw new IllegalArgumentException("총 주문 금액이 일치하지 않습니다.");
        }
    }

    private void validateRestaurantSellsProducts(Restaurant restaurant,
                                                 RestaurantValidationApplicationRequest restaurantValidationRequest,
                                                 List<UUID> productIds) {
        List<RestaurantProduct> restaurantProducts =
                restaurantProductRepository.findByRestaurantIdAndProductIdIn(
                        restaurant.getRestaurantId(), productIds);
        Set<UUID> restaurantProductSet = restaurantProducts.stream()
                .map(RestaurantProduct::getProductId)
                .collect(Collectors.toSet());

        Set<UUID> notSellable = productIds.stream()
                .filter(id -> !restaurantProductSet.contains(id))
                .collect(Collectors.toSet());
        if (!notSellable.isEmpty()) {
            log.warn("해당 레스토랑이 판매하지 않는 상품이 있습니다. Restaurant Id : {}, Product Id : {}, Saga Id : {}",
                    restaurant.getRestaurantId(), notSellable, restaurantValidationRequest.getSagaId());
            throw new IllegalArgumentException("해당 레스토랑이 판매하지 않는 상품이 있습니다.");
        }
    }

    private Map<UUID, RestaurantValidationApplicationRequest.Item> mergeDuplicateItems(
            List<RestaurantValidationApplicationRequest.Item> items) {
        Map<UUID, RestaurantValidationApplicationRequest.Item> result = new HashMap<>();
        for (RestaurantValidationApplicationRequest.Item item : items) {
            result.merge(
                    item.getProductId(),
                    item,
                    (oldV, newV) -> RestaurantValidationApplicationRequest.Item.builder()
                            .productId(oldV.getProductId())
                            .price(oldV.getPrice())
                            .quantity(oldV.getQuantity() + newV.getQuantity())
                            .build()
            );
        }
        return result;
    }

}
