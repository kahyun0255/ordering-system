package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.presentation.request.CreateRestaurantRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("RestaurantManagementController 레스토랑 업데이트 통합 테스트")
class RestaurantManagementControllerUpdateTest extends ControllerTestSupport {

    private final UUID userId = UUID.randomUUID();

    @DisplayName("성공적으로 정보를 업데이트한다.")
    @Test
    void updateRestaurantInfoSuccessfully() throws Exception {
        //given
        ownerRepository.save(Owner.builder()
                .userId(userId)
                .name("테스트유저")
                .build());

        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("레스토랑 이름")
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/restaurants")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("레스토랑이 성공적으로 생성되었습니다."));

        assertThat(restaurantRepository.count()).isEqualTo(1L);
        assertThat(restaurantOwnershipRepository.count()).isEqualTo(1L);
        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(1L);
    }

    @DisplayName("저장된 레스토랑 오너가 없으면 레스토랑 생성에 실패하고, 예외가 발생한다.")
    @Test
    void failToCreateRestaurant_whenOwnerNotFound() throws Exception {
        //given
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("레스토랑 이름")
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/restaurants")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        assertThat(restaurantRepository.count()).isEqualTo(0L);
        assertThat(restaurantOwnershipRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
    }

    @DisplayName("액세스 토큰이 없으면 레스토랑 생성에 실패하고, 예외가 발생한다.")
    @Test
    void failToCreateRestaurant_whenAccessTokenMissing() throws Exception {
        //given
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("레스토랑 이름")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        assertThat(restaurantRepository.count()).isEqualTo(0L);
        assertThat(restaurantOwnershipRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
    }

    @DisplayName("레스토랑 이름이 없으면 레스토랑 생성에 실패하고, 예외가 발생한다.")
    @Test
    void failToCreateRestaurant_whenNameIsMissing() throws Exception {
        //given
        ownerRepository.save(Owner.builder()
                .userId(userId)
                .name("테스트유저")
                .build());

        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .build();

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/restaurants")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 레스토랑 이름은 필수입니다."));

        assertThat(restaurantRepository.count()).isEqualTo(0L);
        assertThat(restaurantOwnershipRepository.count()).isEqualTo(0L);
        assertThat(restaurantUpdateOutboxRepository.count()).isEqualTo(0L);
    }

}
