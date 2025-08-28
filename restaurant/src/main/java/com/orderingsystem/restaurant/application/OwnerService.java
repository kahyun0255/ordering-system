package com.orderingsystem.restaurant.application;

import com.orderingsystem.restaurant.application.dto.request.RestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.ProcessedMessageRepository;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final ProcessedMessageRepository processedMessageRepository;

    @Transactional
    public void createOwner(RestaurantOwnerApplicationRequest restaurantOwnerApplicationRequest) {
        if (markIfNotProcessed(restaurantOwnerApplicationRequest, MessageType.OWNER_CREATE)) {
            return;
        }

        log.info("레스토랑 오너를 생성합니다. Owner Id : {}", restaurantOwnerApplicationRequest.getId());

        ownerRepository.save(Owner.builder()
                .userId(restaurantOwnerApplicationRequest.getId())
                .name(restaurantOwnerApplicationRequest.getUsername())
                .build());
    }

    @Transactional
    public void deleteOwner(RestaurantOwnerApplicationRequest restaurantOwnerApplicationRequest) {
        if (markIfNotProcessed(restaurantOwnerApplicationRequest, MessageType.OWNER_DELETE)) {
            return;
        }

        log.info("레스토랑 오너를 삭제합니다. Owner Id : {}", restaurantOwnerApplicationRequest.getId());

        ownerRepository.deleteById(restaurantOwnerApplicationRequest.getId());
    }

    private boolean markIfNotProcessed(RestaurantOwnerApplicationRequest restaurantOwnerApplicationRequest,
                                       MessageType ownerCreate) {
        int inserted = processedMessageRepository.insertIgnore(
                restaurantOwnerApplicationRequest.getId(),
                ownerCreate.name(),
                ZonedDateTime.now()
        );

        if (inserted == 0) {
            log.info("이미 처리된 Owner {} 메시지입니다. Owner Id : {}", ownerCreate, restaurantOwnerApplicationRequest.getId());
            return true;
        }
        return false;
    }

}
