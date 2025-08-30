package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.orderingsystem.restaurant.application.dto.request.RestaurantOwnerApplicationRequest;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.outbox.MessageType;
import com.orderingsystem.restaurant.domain.model.outbox.ProcessedMessage;
import java.time.Instant;
import java.time.ZonedDateTime;
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

    @DisplayName("이미 처리한 오너 생성 메시지는 다시 처리하지 않는다.")
    @Test
    void shouldNotProcessOwnerCreationMessageAgain_whenAlreadyProcessed() {
        //given
        UUID ownerId = UUID.randomUUID();

        RestaurantOwnerApplicationRequest request = RestaurantOwnerApplicationRequest.builder()
                .id(ownerId)
                .username("테스트유저")
                .createdAt(Instant.now())
                .build();

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(ownerId)
                .messageType(MessageType.OWNER_CREATE)
                .processedAt(ZonedDateTime.now())
                .build());

        //when
        ownerService.createOwner(request);

        //then
        assertThat(ownerRepository.count()).isEqualTo(0L);
        assertThat(ownerRepository.findById(ownerId)).isNotPresent();
    }

    @DisplayName("레스토랑 오너를 성공적으로 삭제한다.")
    @Test
    void deleteOwner() {
        //given
        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("테스트 유저")
                .build());
        assertThat(ownerRepository.findById(ownerId)).isPresent();

        RestaurantOwnerApplicationRequest request = RestaurantOwnerApplicationRequest.builder()
                .id(ownerId)
                .username("테스트유저")
                .createdAt(Instant.now())
                .build();

        //when
        ownerService.deleteOwner(request);

        //then
        assertThat(ownerRepository.findById(ownerId)).isNotPresent();
    }

    @DisplayName("이미 처리한 오너 삭제 메시지는 다시 처리하지 않는다.")
    @Test
    void shouldNotProcessOwnerDeletionMessageAgain_whenAlreadyProcessed() {
        //given
        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("테스트 유저")
                .build());

        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(ownerId)
                .messageType(MessageType.OWNER_DELETE)
                .processedAt(ZonedDateTime.now())
                .build());

        RestaurantOwnerApplicationRequest request = RestaurantOwnerApplicationRequest.builder()
                .id(ownerId)
                .username("테스트유저")
                .createdAt(Instant.now())
                .build();

        assertThat(ownerRepository.findById(ownerId)).isPresent();

        //when
        ownerService.deleteOwner(request);

        //then
        assertThat(ownerRepository.findById(ownerId)).isPresent();
    }

}