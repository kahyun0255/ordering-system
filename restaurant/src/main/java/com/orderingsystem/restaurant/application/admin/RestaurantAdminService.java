package com.orderingsystem.restaurant.application.admin;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantAdminService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public UpdateRestaurantResponse updateRestaurant(UUID userId,
                                                     UUID restaurantId,
                                                     UpdateRestaurantApplicationRequest request) {
        Restaurant restaurant = findRestaurant(restaurantId);

        log.info("관리자가 레스토랑 정보 업데이트 요청. "
                        + "RestaurantId : {}, 요청 User Id : {}, 기존 name : {}, 기존 status : {}, 변경 요청 정보 : {}",
                restaurantId, userId, restaurant.getName(), restaurant.getStatus(), request.toString());

        updateRestaurantInfo(request, restaurant);

        log.info("레스토랑 정보 업데이트 완료. Restaurant Id : {}, 변경을 요청한 User Id : {}, name : {}, status : {}",
                restaurantId, userId, restaurant.getName(), restaurant.getStatus());

        return buildResponse(restaurantId, restaurant);
    }

    private Restaurant findRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다."));
    }

    private void updateRestaurantInfo(UpdateRestaurantApplicationRequest request, Restaurant restaurant) {
        updateNameIfChanged(request, restaurant);
        updateStatusIfChanged(request, restaurant);
    }

    private void updateNameIfChanged(UpdateRestaurantApplicationRequest request, Restaurant restaurant) {
        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equals(restaurant.getName())) {
                restaurant.updateName(newName);
            }
        }
    }

    private void updateStatusIfChanged(UpdateRestaurantApplicationRequest request, Restaurant restaurant) {
        if (request.getStatus() != null && !request.getStatus().equals(restaurant.getStatus())) {
            restaurant.updateStatusByAdmin(request.getStatus());
        }
    }

    private UpdateRestaurantResponse buildResponse(UUID restaurantId, Restaurant restaurant) {
        return UpdateRestaurantResponse.builder()
                .restaurantId(restaurantId)
                .name(restaurant.getName())
                .status(restaurant.getStatus())
                .build();
    }

}
