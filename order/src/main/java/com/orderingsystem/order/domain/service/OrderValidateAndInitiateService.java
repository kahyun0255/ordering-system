package com.orderingsystem.order.domain.service;

import com.orderingsystem.order.domain.event.OrderCreateEvent;
import com.orderingsystem.order.domain.exception.OrderDomainException;
import com.orderingsystem.order.domain.model.Order;
import com.orderingsystem.order.domain.model.OrderItem;
import com.orderingsystem.order.domain.model.Product;
import com.orderingsystem.order.domain.model.Restaurant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderValidateAndInitiateService {

    public OrderCreateEvent validateAndInitiate(Order order, Restaurant restaurant) {
        validateRestaurant(restaurant);
        setOrderProductInformation(order, restaurant);
        order.validateOrder();
        order.initializeOrder();
        log.info("주문 생성. Order ID : {}", order.getId());
        return new OrderCreateEvent(order, ZonedDateTime.now());
    }

    private void validateRestaurant(Restaurant restaurant) {
        if (!restaurant.isActive()) {
            throw new OrderDomainException("restaurant Id : " + restaurant.getRestaurantId() + " active 상태가 아닙니다.");
        }
    }

    private void setOrderProductInformation(Order order, Restaurant restaurant) {
        List<OrderItem> orderItems = order.getItems();
        List<Product> restaurantProducts = restaurant.getProducts();

        for (OrderItem orderItem : orderItems) {
            Product currentProduct = orderItem.getProduct();

            for (Product restaurantProduct : restaurantProducts) {
                if (currentProduct.equals(restaurantProduct)) {
                    currentProduct.updateWithConfirmedNameAndPrice(restaurantProduct.getName(),
                            restaurantProduct.getPrice());
                    break;
                }
            }
        }
    }
}
