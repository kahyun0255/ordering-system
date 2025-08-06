package com.orderingsystem.restaurant.presentation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.application.RestaurantQueryService;
import com.orderingsystem.restaurant.application.dto.response.ProductInfoResponse;
import com.orderingsystem.restaurant.application.dto.response.RestaurantInfoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RestaurantQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantQueryService restaurantQueryService;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId1 = UUID.randomUUID();
    private final UUID productId2 = UUID.randomUUID();

    @DisplayName("레스토랑이 판매하는 상품 목록을 조회할 수 있다.")
    @Test
    void getRestaurantInfoTest() throws Exception {
        //given
        RestaurantInfoResponse response = RestaurantInfoResponse.builder()
                .restaurantId(restaurantId)
                .active(true)
                .products(List.of(
                        new ProductInfoResponse(productId1, "product1", BigDecimal.valueOf(1000), true),
                        new ProductInfoResponse(productId2, "product2", BigDecimal.valueOf(2000), true)
                ))
                .build();

        given(restaurantQueryService.getRestaurantInfo(eq(restaurantId), anyList()))
                .willReturn(response);

        //when, then
        mockMvc.perform(get("/api/restaurant/{restaurantId}/products", restaurantId)
                        .param("productIds", productId1.toString(), productId2.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(restaurantId.toString()))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products[0].productId").value(productId1.toString()))
                .andExpect(jsonPath("$.products[1].productId").value(productId2.toString()))
                .andExpect(jsonPath("$.products[1].active").value(true));
    }
}
