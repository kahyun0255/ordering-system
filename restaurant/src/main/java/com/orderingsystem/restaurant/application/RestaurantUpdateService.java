package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.event.restaruant.UpdatedRestaurantEvent;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantUpdateService {

    @Transactional
    public UpdatedRestaurantEvent update(UpdateRestaurantApplicationRequest request, Restaurant restaurant) {
        boolean changed = false;

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equals(restaurant.getName())) {
                restaurant.updateName(request.getName());
                changed = true;
            }
        }

        if (request.getStatus() != null && !request.getStatus().equals(restaurant.getStatus())) {
            changed = true;
            restaurant.updateStatusByOwner(request.getStatus());
        }

        return changed ? new UpdatedRestaurantEvent(restaurant, ZonedDateTime.now()) : null;
    }

}
