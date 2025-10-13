package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.presentation.request.UpdateRestaurantRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("RestaurantManagementController 레스토랑 업데이트 통합 테스트")
class RestaurantManagementControllerUpdateTest extends ControllerTestSupport {

    private final UUID userId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
                .build());

        ownerRepository.save(Owner.builder()
                .userId(userId)
                .name("테스트유저")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .restaurantId(restaurantId)
                .ownerId(userId)
                .build());
    }

    @DisplayName("성공적으로 정보를 업데이트한다.")
    @Test
    void updateRestaurantInfoSuccessfully() throws Exception {
        //given
        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").isNotEmpty())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.status").value(request.getStatus().name()))
                .andExpect(jsonPath("$.message").value("성공적으로 변경 되었습니다."));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(restaurantId);
        assertThat(updatedRestaurant).isPresent();
        assertThat(updatedRestaurant.get().getStatus()).isEqualTo(RestaurantStatus.PRE_OPEN);
        assertThat(updatedRestaurant.get().getName()).isEqualTo(request.getName());
    }

    @DisplayName("저장된 레스토랑 오너가 없으면 레스토랑 정보 변경에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurant_whenOwnerNotFound() throws Exception {
        //given
        UUID notOwnerId = UUID.randomUUID();

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build();

        String token = buildToken(notOwnerId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        assertThat(restaurantRepository.findById(restaurantId).get().getName()).isNotEqualTo(request.getStatus());
    }

    @DisplayName("액세스 토큰이 없으면 레스토랑 정보 변경에 실패하고, 예외가 발생한다.")
    @Test
    void failToUpdateRestaurant_whenAccessTokenMissing() throws Exception {
        //given
        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        assertThat(restaurantRepository.findById(restaurantId).get().getName()).isNotEqualTo(request.getStatus());
    }

    @DisplayName("해당 레스토랑을 소유한 사용자가 아니면 레스토랑 정보 변경에 실패하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsNotOwnerDuringUpdate() throws Exception {
        //given
        UUID notRestaurantOwnerId = UUID.randomUUID();

        ownerRepository.save(Owner.builder()
                .userId(notRestaurantOwnerId)
                .name("레스토랑 오너가 아님")
                .build());

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build();

        String token = buildToken(notRestaurantOwnerId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 수정 할 권한이 없습니다."));
    }

    @DisplayName("현재 레스토랑 상태가 사용자가 변경할 수 없는 상태라면, 정보 변경에 실패하고 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantStatusIsNotUpdatable() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .status(RestaurantStatus.PENDING_APPROVAL)
                .name("사용자가 변경 불가능한 현재 상태")
                .build();

        restaurantRepository.save(restaurant);

        restaurantOwnershipRepository.save(
                RestaurantOwnership.builder()
                        .restaurantId(restaurant.getRestaurantId())
                        .ownerId(userId)
                        .build());

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.PRE_OPEN)
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurant.getRestaurantId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("해당 상태로 변경이 불가능합니다."));

        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.PENDING_APPROVAL);
    }

    @DisplayName("변경 요청 상태가 사용자가 변경할 수 없는 상태라면, 정보 변경에 실패하고 예외가 발생한다.")
    @Test
    void shouldFailToUpdate_whenStatusIsRestricted() throws Exception {
        //given
        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("업데이트 레스토랑 이름")
                .status(RestaurantStatus.DELETED)
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("해당 상태로 변경이 불가능합니다."));

        assertThat(restaurantRepository.findById(restaurantId).get().getStatus()).isEqualTo(
                RestaurantStatus.ACTIVE);
    }

}
