package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.presentation.request.UpdateProductRequest;
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
import org.springframework.http.MediaType;

class ProductControllerUpdateTest extends ControllerTestSupport {

    private final UUID ownerId = UUID.randomUUID();
    private final UUID nonOwnerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final Product product = Product.builder()
            .productId(UUID.randomUUID())
            .available(true)
            .price(new Money(BigDecimal.valueOf(1000.00)))
            .quantity(100000)
            .name("상품")
            .build();

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Owner owner = Owner.builder()
                .name("owner")
                .userId(ownerId)
                .build();
        ownerRepository.save(owner);

        Owner nonOwner = Owner.builder()
                .name("Not the Owner")
                .userId(nonOwnerId)
                .build();
        ownerRepository.save(nonOwner);

        RestaurantOwnership restaurantOwnership = RestaurantOwnership.builder()
                .ownerId(ownerId)
                .restaurantId(restaurantId)
                .build();
        restaurantOwnershipRepository.save(restaurantOwnership);

        productRepository.save(product);
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        productRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑의 소유자일경우 관리자 승인 대기, 영업 전, 영업 중, 일시 휴업 상태의 레스토랑의 상품 정보 변경이 가능하다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideUpdatableRestaurantStatuses")
    void shouldUpdateProduct_whenOwnerAndRestaurantStatusIsUpdatable(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.price").value(request.getPrice().toString()))
                .andExpect(jsonPath("$.available").value(request.getAvailable()))
                .andExpect(jsonPath("$.quantity").value(request.getQuantity()));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isZero();
    }

    private static Stream<Arguments> provideUpdatableRestaurantStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 폐업, 영업 정지 상태의 레스토랑의 상품은 정보 변경이 불가능하고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideRestrictedStatusesForProductUpdate")
    void shouldNotUpdateProduct_whenOwnerAndRestaurantStatusIsRestricted(String status,
                                                                         RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("현재 상태의 레스토랑에서는 상품을 관리할 수 없습니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isOne();
    }

    private static Stream<Arguments> provideRestrictedStatusesForProductUpdate() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑이 삭제되었으면 404를 반환한다.")
    @Test
    void shouldThrowNotFoundException_whenRestaurantIsDeleted() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.DELETED)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isOne();
    }

    @DisplayName("레스토랑이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldThrowNotFoundException_whenRestaurantDoesNotExist() throws Exception {
        //given
        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + UUID.randomUUID() + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isOne();
    }

    @DisplayName("레스토랑의 소유자가 아니라면 레스토랑에서 판매하는 상품 정보 변경에 실패하고, 403을 반환한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserIsNotOwner() throws Exception {
        //given
        UUID nonOwnerId = UUID.randomUUID();
        ownerRepository.save(Owner.builder()
                .userId(nonOwnerId)
                .name("소유자 아님")
                .build());

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(nonOwnerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("상품을 관리할 권한이 없습니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isOne();
    }

    @DisplayName("정보를 변경할 상품이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldThrowNotFoundException_whenProductDoesNotExist() throws Exception {
        //given
       Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("변경할 상품 이름")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .quantity(100)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isOne();
    }

    @DisplayName("일부 정보만 변경할 수 있다.")
    @Test
    void shouldAllowPartialProductUpdate() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(product.getName()))
                .andExpect(jsonPath("$.price").value(request.getPrice().toString()))
                .andExpect(jsonPath("$.available").value(request.getAvailable()))
                .andExpect(jsonPath("$.quantity").value(product.getQuantity()));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isZero();
        assertThat(after.get().isAvailable()).isEqualTo(request.getAvailable());

        assertThat(after.get().getName()).isEqualTo(product.getName());
        assertThat(after.get().getQuantity()).isEqualTo(product.getQuantity());
    }

    @DisplayName("변경할 이름이 2자 미만이면 정보 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldThrowException_whenNameIsTooShort() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("이")
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("상품 이름은 2자 이상 30자 이하로 입력해주세요."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(product.getName());
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isEqualTo(product.getQuantity());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
    }

    @DisplayName("변경할 이름이 30자 초과면 정보 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldThrowException_whenNameIsTooLong() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("이".repeat(31))
                .price(BigDecimal.valueOf(50.00))
                .available(false)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("상품 이름은 2자 이상 30자 이하로 입력해주세요."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(product.getName());
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isEqualTo(product.getQuantity());
        assertThat(after.get().getQuantity()).isNotEqualTo(request.getQuantity());
    }

    @DisplayName("상품 가격이 0 미만이면 정보 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPriceIsNegative() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .price(BigDecimal.valueOf(-1))
                .available(false)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("상품 가격은 0보다 커야 합니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getPrice()).isEqualTo(product.getPrice());
        assertThat(after.get().getPrice().getAmount()).isNotEqualTo(request.getPrice());
        assertThat(after.get().isAvailable()).isEqualTo(product.isAvailable());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
    }

    @DisplayName("상품 재고가 음수라면 정보 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldThrowException_whenStockIsNegative() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        UpdateProductRequest request = UpdateProductRequest.builder()
                .quantity(-1)
                .available(false)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/restaurants/" + restaurantId + "/products/" + product.getProductId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("상품 재고는 0 이상이어야 합니다."));

        //then
        Optional<Product> after = productRepository.findById(product.getProductId());
        assertThat(after).isPresent();
        assertThat(after.get().getPrice()).isEqualTo(product.getPrice());
        assertThat(after.get().getPrice().getAmount()).isNotEqualTo(request.getPrice());
        assertThat(after.get().isAvailable()).isEqualTo(product.isAvailable());
        assertThat(after.get().isAvailable()).isNotEqualTo(request.getAvailable());
    }

}
