package com.orderingsystem.restaurant.application;

import static com.orderingsystem.common.saga.SagaConstants.RESTAURANT_CREATE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.outbox.OutboxStatus;
import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.application.dto.response.UpdateRestaurantResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.repository.OwnerRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantOwnershipRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.repository.outbox.RestaurantUpdateOutboxRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RestaurantManagementFacadeUpdateTest {

    @Autowired
    private RestaurantManagementFacade restaurantManagementFacade;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantOwnershipRepository restaurantOwnershipRepository;

    @Autowired
    private RestaurantUpdateOutboxRepository restaurantUpdateOutboxRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("레스토랑 오너")
                .build());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .active(true)
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());
    }

    @AfterEach
    void tearDown() {
        ownerRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        restaurantOwnershipRepository.deleteAllInBatch();
        restaurantUpdateOutboxRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑 이름 업데이트에 성공한다.")
    @Test
    void updateRestaurantNameSuccessfully() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .active(null)
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());

        //when
        restaurantManagementFacade.updateRestaurant(request, restaurantId.toString(), ownerId);

        //then
        Optional<Restaurant> afterRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(afterRestaurant).isPresent();
        assertThat(afterRestaurant.get().getName()).isEqualTo(request.getName());

        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isNotEmpty();
    }

    @DisplayName("레스토랑 활성화 상태 업데이트에 성공한다.")
    @Test
    void updateRestaurantActivationSuccessfully() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(null)
                .active(false)
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getActive()).isNotEqualTo(request.getActive());

        //when
        restaurantManagementFacade.updateRestaurant(request, restaurantId.toString(), ownerId);

        //then
        Optional<Restaurant> afterRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(afterRestaurant).isPresent();
        assertThat(afterRestaurant.get().getActive()).isEqualTo(request.getActive());

        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isNotEmpty();
    }

    @DisplayName("레스토랑 활성화 상태 및 이름 업데이트에 성공한다.")
    @Test
    void updateRestaurantActivationAndNameSuccessfully() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경이름")
                .active(false)
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());
        assertThat(beforeRestaurant.get().getActive()).isNotEqualTo(request.getActive());

        //when
        UpdateRestaurantResponse response = restaurantManagementFacade.updateRestaurant(request,
                restaurantId.toString(), ownerId);

        //then
        Optional<Restaurant> afterRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(afterRestaurant).isPresent();
        assertThat(afterRestaurant.get().getName()).isEqualTo(request.getName());
        assertThat(afterRestaurant.get().getActive()).isEqualTo(request.getActive());

        assertThat(response.getName()).isEqualTo(afterRestaurant.get().getName());
        assertThat(response.getActive()).isEqualTo(afterRestaurant.get().getActive());
        assertThat(response.getMessage()).isEqualTo("성공적으로 변경 되었습니다.");

        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isNotEmpty();
    }

    @DisplayName("변경 사항이 없으면 변경된 내용이 없다는 메시지를 반환하고, outbox에 저장되지 않는다.")
    @Test
    void shouldNotUpdateWhenNoChanges() {
        //given
        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(beforeRestaurant.get().getName())
                .active(beforeRestaurant.get().getActive())
                .build();

        //when
        UpdateRestaurantResponse response = restaurantManagementFacade.updateRestaurant(request,
                restaurantId.toString(), ownerId);

        //then
        assertThat(response.getMessage()).isEqualTo("변경된 내용이 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isEmpty();
    }

    @DisplayName("레스토랑 이름이 공백이라면 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantName_whenNameIsBlank() {
        //given
        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(" ")
                .active(false)
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());
        assertThat(beforeRestaurant.get().getActive()).isNotEqualTo(request.getActive());

        //when, then
        assertThatThrownBy(() -> restaurantManagementFacade.updateRestaurant(request, restaurantId.toString(), ownerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레스토랑 이름은 비어있을 수 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isEmpty();
    }

    @DisplayName("레스토랑 오너가 아니면 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenNotOwner() {
        //given
        UUID notOwnerId = UUID.randomUUID();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .active(false)
                .build();

        Optional<Restaurant> beforeRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(beforeRestaurant).isPresent();
        assertThat(beforeRestaurant.get().getName()).isNotEqualTo(request.getName());
        assertThat(beforeRestaurant.get().getActive()).isNotEqualTo(request.getActive());

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, restaurantId.toString(), notOwnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 오너 정보를 찾을 수 없습니다.");

        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.findByTypeAndOutboxStatus(RESTAURANT_CREATE_NAME,
                OutboxStatus.STARTED)).isEmpty();
    }

    @DisplayName("레스토랑 정보를 찾을 수 없으면 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenRestaurantNotFound() {
        //given
        UUID notRestaurantId = UUID.randomUUID();

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .active(false)
                .build();

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, notRestaurantId.toString(), ownerId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

    @DisplayName("해당 레스토랑을 수정 할 권한이 없으면 레스토랑 정보 업데이트에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurantInfo_whenNotAuthorized() {
        //given
        UUID notOwnershipOwnerId = UUID.randomUUID();

        ownerRepository.save(Owner.builder()
                .userId(notOwnershipOwnerId)
                .name("레스토랑 오너")
                .build());

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경 할 이름")
                .active(false)
                .build();

        //when, then
        assertThatThrownBy(
                () -> restaurantManagementFacade.updateRestaurant(request, restaurantId.toString(), notOwnershipOwnerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("레스토랑 정보를 수정 할 권한이 없습니다.");
    }

}
