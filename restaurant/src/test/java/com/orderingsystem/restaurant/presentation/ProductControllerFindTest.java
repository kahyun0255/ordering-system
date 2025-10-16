package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MvcResult;

class ProductControllerFindTest extends ControllerTestSupport {

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID inactiveProductId = UUID.randomUUID();
    private final Product activeProduct1 = Product.builder()
            .productId(UUID.randomUUID())
            .price(new Money(BigDecimal.valueOf(100.00)))
            .name("판매 가능한 상품 1")
            .available(true)
            .quantity(10)
            .build();

    private final Product activeProduct2 = Product.builder()
            .productId(UUID.randomUUID())
            .price(new Money(BigDecimal.valueOf(20.0)))
            .name("판매 가능한 상품 2")
            .available(true)
            .quantity(30)
            .build();

    @BeforeEach
    void setUp() {
        Product inactiveProduct = Product.builder()
                .productId(inactiveProductId)
                .price(new Money(BigDecimal.valueOf(20.0)))
                .name("판매 불가능한 상품")
                .available(false)
                .quantity(10)
                .build();

        productRepository.saveAll(List.of(activeProduct1, activeProduct2, inactiveProduct));

        RestaurantProduct restaurantProduct1 = getRestaurantProduct(activeProduct1.getProductId());
        RestaurantProduct restaurantProduct2 = getRestaurantProduct(activeProduct2.getProductId());
        RestaurantProduct restaurantProduct3 = getRestaurantProduct(inactiveProductId);
        restaurantProductRepository.saveAll(List.of(restaurantProduct1, restaurantProduct2, restaurantProduct3));
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        productRepository.deleteAllInBatch();
        restaurantProductRepository.deleteAllInBatch();
    }

    @DisplayName("관리자 승인 대기, 영업 전, 영업 중, 일시 휴업 상태의 레스토랑에서 판매가 가능한 상품 전체 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideProductManageableStatuses")
    void shouldReturnProducts_whenRestaurantStatusAllowsProductManagement(String status,
                                                                          RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑 이름")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/products"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        JsonNode content = root.path("content");
        List<ProductResponse> products = objectMapper.readValue(
                content.traverse(),
                new TypeReference<List<ProductResponse>>() {
                }
        );

        assertThat(products).hasSize(2)
                .extracting("productId", "name", "price", "available", "quantity")
                .containsExactlyInAnyOrder(
                        tuple(activeProduct1.getProductId(), activeProduct1.getName(),
                                activeProduct1.getPrice().getAmount(),
                                activeProduct1.isAvailable(), activeProduct1.getQuantity()),
                        tuple(activeProduct2.getProductId(), activeProduct2.getName(),
                                activeProduct2.getPrice().getAmount(),
                                activeProduct2.isAvailable(), activeProduct2.getQuantity())
                );
    }

    private static Stream<Arguments> provideProductManageableStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("폐업, 영업 정지, 삭제 상태의 레스토랑에는 레스토랑에서 판매 가능한 상품 전체 조회가 불가능하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("restaurantStatusesThatCannotExposeProducts")
    void shouldFailToGetProducts_whenRestaurantStatusIsInvalid(String status, RestaurantStatus restaurantStatus) throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑 이름")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        //when, then
        mockMvc.perform(
                        get("/api/restaurants/" + restaurantId + "/products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> restaurantStatusesThatCannotExposeProducts() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED),
                Arguments.of("삭제", RestaurantStatus.DELETED)
        );
    }

    private RestaurantProduct getRestaurantProduct(UUID productId) {
        return RestaurantProduct.builder()
                .productId(productId)
                .restaurantId(restaurantId)
                .build();
    }

}
