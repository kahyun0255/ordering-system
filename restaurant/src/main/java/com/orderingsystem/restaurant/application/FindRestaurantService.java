package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.response.FindAllRestaurantsResponse;
import com.orderingsystem.restaurant.application.dto.response.FindRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .status(restaurant.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<FindAllRestaurantsResponse> findAllRestaurants(Pageable pageable, RestaurantStatus status) {
        if (status != null && status.ownerCanFind(status)) {
            throw new IllegalArgumentException("해당 상태의 레스토랑 조회가 불가능합니다.");
        }

        List<RestaurantStatus> statuses = status == null ?
                List.of(RestaurantStatus.ACTIVE, RestaurantStatus.PRE_OPEN, RestaurantStatus.TEMP_CLOSED) :
                List.of(status);

        Page<Restaurant> restaurants = restaurantRepository.findAllByStatusIn(pageable, statuses);

        return restaurants.map(restaurant ->
                FindAllRestaurantsResponse.builder()
                        .restaurantId(restaurant.getRestaurantId())
                        .restaurantName(restaurant.getName())
                        .status(restaurant.getStatus())
                        .build());
    }

}
