package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.restaurant.application.dto.request.CreateRestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OwnerCreateServiceTest {

    @Autowired
    private OwnerCreateService ownerCreateService;

    @Autowired
    private OwnerRepository ownerRepository;

    private final UUID ownerId = UUID.randomUUID();

    @DisplayName("레스토랑 오너를 성공적으로 저장한다.")
    @Test
    void createOwner() {
        //given
        CreateRestaurantOwnerApplicationRequest request = CreateRestaurantOwnerApplicationRequest.builder()
                .id(ownerId)
                .username("테스트유저")
                .createdAt(Instant.now())
                .build();

        //when
        ownerCreateService.createOwner(request);

        //then
        Optional<Owner> savedOwner = ownerRepository.findById(ownerId);
        assertThat(savedOwner).isPresent();
        assertThat(savedOwner.get().getName()).isEqualTo(request.getUsername());
    }

}