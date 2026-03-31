package com.orderingsystem.restaurant.presentation.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.presentation.ControllerTestSupport;
import com.orderingsystem.restaurant.presentation.request.UpdateRestaurantRequest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RestaurantAdminControllerUpdateTest extends ControllerTestSupport {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @AfterEach
    void tearDown() {
        restaurantRepository.deleteAllInBatch();
    }

    @DisplayName("관리자는 레스토랑 소유자가 아니더라도 레스토랑 정보를 변경할 수 있다.")
    @Test
    void shouldSucceedUpdatingRestaurant_whenUserIsAdminButNotOwner() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑1")
                .status(RestaurantStatus.ACTIVE)
                .build();

        restaurantRepository.save(restaurant);

        String token = buildToken(adminUserId, UserType.ADMIN);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("변경할 레스토랑 이름")
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when, then
        mockMvc.perform(patch("/api/admin/restaurants/" + restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(restaurantId.toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.status").value(request.getStatus().name()));

        Restaurant after = restaurantRepository.findById(restaurantId)
                .orElseThrow();

        assertThat(after.getName()).isEqualTo(request.getName());
        assertThat(after.getStatus()).isEqualTo(request.getStatus());
    }

    @DisplayName("관리자는 레스토랑 소유자가 아니더라도 레스토랑 이름만 변경할 수 있다.")
    @Test
    void shouldSucceedUpdatingRestaurantName_whenUserIsAdminButNotOwner() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑1")
                .status(RestaurantStatus.ACTIVE)
                .build();

        restaurantRepository.save(restaurant);

        String token = buildToken(adminUserId, UserType.ADMIN);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("변경할 레스토랑 이름")
                .build();

        //when, then
        mockMvc.perform(patch("/api/admin/restaurants/" + restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(restaurantId.toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.status").value(restaurant.getStatus().name()));

        Restaurant after = restaurantRepository.findById(restaurantId)
                .orElseThrow();

        assertThat(after.getName()).isEqualTo(request.getName());
        assertThat(after.getStatus()).isNotNull();
        assertThat(after.getStatus()).isEqualTo(restaurant.getStatus());
    }

    @DisplayName("관리자는 레스토랑 소유자가 아니더라도 레스토랑 상태만 변경할 수 있다.")
    @Test
    void shouldSucceedUpdatingRestaurantStatus_whenUserIsAdminButNotOwner() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑1")
                .status(RestaurantStatus.ACTIVE)
                .build();

        restaurantRepository.save(restaurant);

        String token = buildToken(adminUserId, UserType.ADMIN);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when, then
        mockMvc.perform(patch("/api/admin/restaurants/" + restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(restaurantId.toString()))
                .andExpect(jsonPath("$.name").value(restaurant.getName()))
                .andExpect(jsonPath("$.status").value(request.getStatus().name()));

        Restaurant after = restaurantRepository.findById(restaurantId)
                .orElseThrow();

        assertThat(after.getStatus()).isEqualTo(request.getStatus());
        assertThat(after.getName()).isNotNull();
        assertThat(after.getName()).isEqualTo(restaurant.getName());
    }

    @DisplayName("관리자가 레스토랑 정보 변경을 요청할 때, 레스토랑이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404NotFound_whenAdminRequestsUpdatingNonExistentRestaurant() throws Exception {
        //given
        UUID adminUserId = UUID.randomUUID();
        String token = buildToken(adminUserId, UserType.ADMIN);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("변경할 레스토랑 이름")
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when, then
        mockMvc.perform(patch("/api/admin/restaurants/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));
    }

    @DisplayName("관리자가 레스토랑 정보 변경을 요청할 때, 이름을 공백으로 변경하길 요청하면 400을 반환한다.")
    @Test
    void shouldReturn400BadRequest_whenAdminRequestsUpdatingWithBlankName() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        String token = buildToken(adminUserId, UserType.ADMIN);

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑1")
                .status(RestaurantStatus.ACTIVE)
                .build();

        restaurantRepository.save(restaurant);

        UpdateRestaurantRequest request = UpdateRestaurantRequest.builder()
                .name("     ")
                .build();

        //when, then
        mockMvc.perform(patch("/api/admin/restaurants/" + restaurantId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("레스토랑 이름은 비어있을 수 없습니다."));
    }

}
