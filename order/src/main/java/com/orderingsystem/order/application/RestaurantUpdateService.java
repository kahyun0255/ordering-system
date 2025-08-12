package com.orderingsystem.order.application;

import com.orderingsystem.order.application.dto.request.RestaurantUpdateApplicationRequest;
import com.orderingsystem.order.domain.model.restaurant.Restaurant;
import com.orderingsystem.order.domain.repository.restaurant.RestaurantRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantUpdateService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void create(RestaurantUpdateApplicationRequest restaurantUpdateApplicationRequest) {
        log.info("주문 도메인 레스토랑 생성. Restaurant Id : {}", restaurantUpdateApplicationRequest.getRestaurantId());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(UUID.fromString(restaurantUpdateApplicationRequest.getRestaurantId()))
                .name(restaurantUpdateApplicationRequest.getName())
                .active(restaurantUpdateApplicationRequest.isActive())
                .build());
    }
}
