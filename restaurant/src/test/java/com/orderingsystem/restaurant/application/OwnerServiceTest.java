package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.restaurant.application.dto.request.RestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OwnerServiceTest extends ApplicationTestSupport {

    @Autowired
    private OwnerService ownerService;

    private final UUID ownerId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 오너를 성공적으로 저장한다.")
    @Test
    void createOwner() {
        //given
        RestaurantOwnerApplicationRequest request = RestaurantOwnerApplicationRequest.builder()
                .id(ownerId)
                .username("테스트유저")
                .createdAt(Instant.now())
                .build();

        //when
        ownerService.createOwner(request);

        //then
        Optional<Owner> savedOwner = ownerRepository.findById(ownerId);
        assertThat(savedOwner).isPresent();
        assertThat(savedOwner.get().getName()).isEqualTo(request.getUsername());
    }

}