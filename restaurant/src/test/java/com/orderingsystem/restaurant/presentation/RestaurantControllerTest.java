package com.orderingsystem.restaurant.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("RestaurantControllerTest 레스토랑 상세 조회 통합 테스트")
class RestaurantControllerTest extends ControllerTestSupport {

    @DisplayName("레스토랑을 상세 조회한다.")
    @Test
    void getRestaurantDetailsSuccessfully() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .active(true)
                .build();
        restaurantRepository.save(restaurant);

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurant.getRestaurantId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(restaurant.getName()))
                .andExpect(jsonPath("$.active").value(restaurant.getActive()));
    }

    @DisplayName("레스토랑이 존재하지 않을 경우 404 Not Found 응답을 반환한다.")
    @Test
    void failToGetRestaurantDetails_whenRestaurantNotFound() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .active(true)
                .build();

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurant.getRestaurantId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));
    }

}
