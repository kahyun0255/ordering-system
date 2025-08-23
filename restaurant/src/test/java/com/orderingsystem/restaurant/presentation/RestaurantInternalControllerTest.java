package com.orderingsystem.restaurant.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.application.RestaurantInternalFacade;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.presentation.request.RestaurantValidationRequest;
import com.orderingsystem.restaurant.presentation.request.RestaurantValidationRequest.Item;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RestaurantInternalControllerTest extends ControllerTestSupport {

    @MockitoBean
    private RestaurantInternalFacade restaurantInternalFacade;

    @DisplayName("주문 검증에 성공한다.")
    @Test
    void validateOrderSuccessfully() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        RestaurantValidationRequest request = RestaurantValidationRequest.builder()
                .sagaId(UUID.randomUUID())
                .items(List.of(
                        Item.builder()
                                .productId(UUID.randomUUID())
                                .price(new BigDecimal("10.00"))
                                .quantity(1)
                                .build()
                ))
                .totalPrice(new BigDecimal("10.00"))
                .build();

        doNothing().when(restaurantInternalFacade).validateRestaurant(eq(restaurantId), any());

        //when, then
        mockMvc.perform(post("/api/internal/restaurants/{restaurantId}/validate", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(restaurantInternalFacade, times(1))
                .validateRestaurant(eq(restaurantId), any());
    }

    @DisplayName("도메인 검증 실패시 400을 반환한다.")
    @Test
    void validate_badRequest() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        RestaurantValidationRequest request = RestaurantValidationRequest.builder()
                .sagaId(UUID.randomUUID())
                .items(List.of())
                .totalPrice(new BigDecimal("10.00"))
                .build();

        doThrow(new IllegalArgumentException("주문 상품이 비어있습니다."))
                .when(restaurantInternalFacade)
                .validateRestaurant(eq(restaurantId), any());

        //when, then
        mockMvc.perform(post("/api/internal/restaurants/{id}/validate", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("주문 상품이 비어있습니다.")));
    }

    @DisplayName("레스토랑이 존재하지 않으면 404를 반환한다.")
    @Test
    void notFoundRestaurant() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        RestaurantValidationRequest request = RestaurantValidationRequest.builder()
                .sagaId(UUID.randomUUID())
                .items(List.of())
                .totalPrice(new BigDecimal("10.00"))
                .build();

        doThrow(new RestaurantNotFoundException("레스토랑 정보를 찾을 수 없습니다."))
                .when(restaurantInternalFacade)
                .validateRestaurant(eq(restaurantId), any());

        //when, then
        mockMvc.perform(post("/api/internal/restaurants/{id}/validate", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("레스토랑 정보를 찾을 수 없습니다.")));
    }

}
