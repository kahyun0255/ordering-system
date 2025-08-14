package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindRestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public FindRestaurantResponse findRestaurant(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> {
                    log.warn("레스토랑 정보를 찾을 수 없습니다. Restaurant Id : {}", restaurantId);
                    return new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다.");
                });

        return FindRestaurantResponse.builder()
                .name(restaurant.getName())
                .active(restaurant.getActive())
                .build();
    }

}
