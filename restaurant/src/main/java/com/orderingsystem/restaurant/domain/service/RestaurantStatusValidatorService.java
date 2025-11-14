package com.orderingsystem.restaurant.domain.service;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RestaurantStatusValidatorService {

    public void validateActive(Restaurant restaurant, UUID orderId) {
        if (!restaurant.getStatus().equals(RestaurantStatus.ACTIVE)) {
            log.info("[{}] 레스토랑이 주문을 받을 수 없는 상태입니다. 상태 : {} Order Id : {}",
                    restaurant.getRestaurantId(), restaurant.getStatus(), orderId);
            throw new IllegalArgumentException("레스토랑이 주문을 받을 수 없는 상태입니다.");
        }
    }

}
