package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.RestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;

    @Transactional
    public void createOwner(RestaurantOwnerApplicationRequest restaurantOwnerApplicationRequest) {
        log.info("레스토랑 오너를 생성합니다. Owner Id : {}", restaurantOwnerApplicationRequest.getId());

        ownerRepository.save(Owner.builder()
                .userId(restaurantOwnerApplicationRequest.getId())
                .name(restaurantOwnerApplicationRequest.getUsername())
                .build());
    }

    @Transactional
    public void deleteOwner(RestaurantOwnerApplicationRequest restaurantOwnerApplicationRequest) {
        log.info("레스토랑 오너를 삭제합니다. Owner Id : {}", restaurantOwnerApplicationRequest.getId());

        ownerRepository.deleteById(restaurantOwnerApplicationRequest.getId());
    }

}
