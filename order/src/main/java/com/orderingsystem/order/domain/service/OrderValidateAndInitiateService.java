package com.orderingsystem.order.domain.service;

import com.orderingsystem.order.application.dto.ProductInfo;
import com.orderingsystem.order.application.dto.RestaurantInfo;
import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderValidateAndInitiateService {

    public OrderCreateEvent validateAndInitiate(Order order, RestaurantInfo restaurantInfo,
                                                          List<String> failureMessages) {
        validateRestaurant(restaurantInfo, failureMessages);
        setOrderProductInformation(order, restaurantInfo);
        order.validateOrder(failureMessages);
        order.initializeOrder();

        log.info("주문 생성. Order Id : {}", order.getId());

        return new OrderCreateEvent(order, ZonedDateTime.now());
    }

    private void validateRestaurant(RestaurantInfo restaurantInfo, List<String> failureMessages) {
        if (!restaurantInfo.isActive()) {
            log.warn("restaurant Id : {} active 상태가 아닙니다.", restaurantInfo.getRestaurantId());
            failureMessages.add("restaurant Id : " + restaurantInfo.getRestaurantId() + " active 상태가 아닙니다.");
        }
    }

    private void setOrderProductInformation(Order order, RestaurantInfo restaurantInfo) {
        List<OrderItem> orderItems = order.getItems();
        List<ProductInfo> restaurantProducts = restaurantInfo.getProducts();

        for (OrderItem orderItem : orderItems) {
            ProductInfo currentProduct = orderItem.getProduct();
            for (ProductInfo restaurantProduct : restaurantProducts) {
                if (currentProduct.equals(restaurantProduct)) {
                    currentProduct.updateWithConfirmedNameAndPrice(restaurantProduct.getName(),
                            restaurantProduct.getPrice(), restaurantProduct.isAvailable());
                    break;
                }
            }
        }
    }
}
