package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.RestaurantOwner;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerCreateService {

    private final RestaurantOwnerRepository restaurantOwnerRepository;

    public void createOwner(CreateRestaurantOwnerApplicationRequest createRestaurantOwnerApplicationRequest) {
        log.info("레스토랑 오너를 생성합니다.");

        restaurantOwnerRepository.save(RestaurantOwner.createOwner(createRestaurantOwnerApplicationRequest.getId()));
    }
}
