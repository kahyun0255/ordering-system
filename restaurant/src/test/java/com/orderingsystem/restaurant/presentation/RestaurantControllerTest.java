package com.orderingsystem.restaurant.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("RestaurantControllerTest 레스토랑 상세 조회 통합 테스트")
class RestaurantControllerTest extends ControllerTestSupport {

    private final Restaurant restaurant1 = Restaurant.builder()
            .restaurantId(UUID.randomUUID())
            .status(RestaurantStatus.ACTIVE)
            .name("레스토랑 1")
            .build();

    private final Restaurant restaurant2 = Restaurant.builder()
            .restaurantId(UUID.randomUUID())
            .status(RestaurantStatus.PRE_OPEN)
            .name("레스토랑 2")
            .build();

    private final  Restaurant restaurant3 = Restaurant.builder()
            .restaurantId(UUID.randomUUID())
            .status(RestaurantStatus.TEMP_CLOSED)
            .name("레스토랑 3")
            .build();

    private final Restaurant restaurant4 = Restaurant.builder()
            .restaurantId(UUID.randomUUID())
            .status(RestaurantStatus.DELETED)
            .name("삭제된 레스토랑")
            .build();

    @BeforeEach
    void setUp() {
        restaurantRepository.saveAll(List.of(restaurant1, restaurant2, restaurant3, restaurant4));
    }

    @DisplayName("레스토랑을 상세 조회한다.")
    @Test
    void getRestaurantDetailsSuccessfully() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurant.getRestaurantId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(restaurant.getName()))
                .andExpect(jsonPath("$.status").value(restaurant.getStatus().name()));
    }

    @DisplayName("레스토랑이 존재하지 않을 경우 404 Not Found 응답을 반환한다.")
    @Test
    void failToGetRestaurantDetails_whenRestaurantNotFound() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(UUID.randomUUID())
                .name("레스토랑 이름")
                .status(RestaurantStatus.ACTIVE)
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

    @DisplayName("별도의 status 없이 레스토랑 전체를 상세 조회 하면 ACTIVE, PRE_OPEN, TEMP_CLOSED 상태의 레스토랑들을 조회한다.")
    @Test
    void getAllRestaurantSuccessfully() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].restaurantId").value(restaurant1.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[0].restaurantName").value(restaurant1.getName()))
                .andExpect(jsonPath("$.content[0].status").value(restaurant1.getStatus().name()))
                .andExpect(jsonPath("$.content[1].restaurantId").value(restaurant2.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[1].restaurantName").value(restaurant2.getName()))
                .andExpect(jsonPath("$.content[1].status").value(restaurant2.getStatus().name()))
                .andExpect(jsonPath("$.content[2].restaurantId").value(restaurant3.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[2].restaurantName").value(restaurant3.getName()))
                .andExpect(jsonPath("$.content[2].status").value(restaurant3.getStatus().name()))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @DisplayName("status를 지정해 조회하면 해당 상태의 레스토랑들을 조회한다.")
    @Test
    void shouldRetrieveRestaurantsBySpecifiedStatusSuccessfully() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .param("status", RestaurantStatus.ACTIVE.name())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].restaurantId").value(restaurant1.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[0].restaurantName").value(restaurant1.getName()))
                .andExpect(jsonPath("$.content[0].status").value(restaurant1.getStatus().name()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @DisplayName("일반 사용자가 조회 불가능한 상태인 DELETED 상태로 status를 조회하면 400을 반환한다.")
    @Test
    void shouldReturnBadRequest_whenStatusIsDeleted() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .param("status", RestaurantStatus.DELETED.name())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("해당 상태의 레스토랑 조회가 불가능합니다."));
    }

    @DisplayName("일반 사용자가 조회 불가능한 상태인 PENDING_APPROVAL 상태로 status를 조회하면 400을 반환한다.")
    @Test
    void shouldReturnBadRequest_whenStatusIsPendingApproval() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .param("status", RestaurantStatus.PENDING_APPROVAL.name())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("해당 상태의 레스토랑 조회가 불가능합니다."));
    }

    @DisplayName("일반 사용자가 조회 불가능한 상태인 SUSPENDED 상태로 status를 조회하면 400을 반환한다.")
    @Test
    void shouldReturnBadRequest_whenStatusIsSuspended() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .param("status", RestaurantStatus.SUSPENDED.name())
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("해당 상태의 레스토랑 조회가 불가능합니다."));
    }

    @DisplayName("음수 페이지 번호 요청 시, 0번 페이지로 대체하여 조회한다.")
    @Test
    void shouldDefaultToFirstPage_whenPageNumberIsNegative() throws Exception {
        //when, then
        mockMvc.perform(
                        get("/api/restaurants")
                                .param("page", "-1")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].restaurantId").value(restaurant1.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[0].restaurantName").value(restaurant1.getName()))
                .andExpect(jsonPath("$.content[0].status").value(restaurant1.getStatus().name()))
                .andExpect(jsonPath("$.content[1].restaurantId").value(restaurant2.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[1].restaurantName").value(restaurant2.getName()))
                .andExpect(jsonPath("$.content[1].status").value(restaurant2.getStatus().name()))
                .andExpect(jsonPath("$.content[2].restaurantId").value(restaurant3.getRestaurantId().toString()))
                .andExpect(jsonPath("$.content[2].restaurantName").value(restaurant3.getName()))
                .andExpect(jsonPath("$.content[2].status").value(restaurant3.getStatus().name()))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

}
