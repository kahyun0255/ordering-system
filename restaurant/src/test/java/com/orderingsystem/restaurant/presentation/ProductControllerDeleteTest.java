package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

class ProductControllerDeleteTest extends ControllerTestSupport {

    @Autowired
    private ProductRepository productRepository;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final Product product = Product.builder()
            .productId(UUID.randomUUID())
            .price(new Money(BigDecimal.valueOf(100.00)))
            .name("상품")
            .quantity(100)
            .available(true)
            .build();

    @BeforeEach
    void setUp() {
        productRepository.save(product);

        restaurantProductRepository.save(RestaurantProduct.builder()
                .restaurantId(restaurantId)
                .productId(product.getProductId())
                .build());

        ownerRepository.save(Owner.builder()
                .userId(ownerId)
                .name("레스토랑 소유자")
                .build());

        restaurantOwnershipRepository.save(RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build());
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        productRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑의 소유자이고, 해당 레스토랑이 '관리자 승인 대기', '영업 전', '영업 중', '일시 휴업' 상태라면 상품을 삭제할 수 있다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideDeletableRestaurantStatuses")
    void shouldDeleteProduct_whenOwnerAndRestaurantStatusAllows(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(1000));

        //when
        mockMvc.perform(
                        delete("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().isAvailable()).isFalse();
    }

    private static Stream<Arguments> provideDeletableRestaurantStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 해당 레스토랑이 '폐업', '영업 정지' 상태라면 상품을 삭제할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("restrictedRestaurantStatusesForProductDeletion")
    void shouldDenyProductDeletion_whenRestaurantStatusIsRestricted(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build());

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("현재 상태의 레스토랑에서는 상품을 관리할 수 없습니다."));

        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().isAvailable()).isTrue();
    }

    private static Stream<Arguments> restrictedRestaurantStatusesForProductDeletion() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑이 삭제되었거나 존재하지 않으면 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideUnavailableOrDeletedRestaurants")
    void shouldAllowProductDeletion_whenOwnerAndRestaurantStatusIsValid(String status,
                                                                        RestaurantStatus restaurantStatus) throws Exception {
        //given
        if (restaurantStatus != null) {
            restaurantRepository.save(Restaurant.builder()
                    .restaurantId(restaurantId)
                    .name("레스토랑")
                    .status(restaurantStatus)
                    .build());
        }

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().isAvailable()).isTrue();
    }

    private static Stream<Arguments> provideUnavailableOrDeletedRestaurants() {
        return Stream.of(
                Arguments.of("삭제", RestaurantStatus.DELETED),
                Arguments.of("존재하지 않음", null)
        );
    }

    @DisplayName("레스토랑의 소유자가 아니라면 상품을 삭제할 수 없다.")
    @Test
    void shouldDenyProductDeletion_whenUserIsNotRestaurantOwner() throws Exception {
        //given
        UUID nonOwnerId = UUID.randomUUID();
        ownerRepository.save(Owner.builder()
                .userId(nonOwnerId)
                .name("소유자 아님")
                .build());

        restaurantRepository.save(Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build());

        String token = buildToken(nonOwnerId, "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("상품을 관리할 권한이 없습니다."));

        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().isAvailable()).isTrue();
    }

}
