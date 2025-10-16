package com.orderingsystem.restaurant.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.presentation.request.CreateProductRequest;
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
import org.springframework.test.web.servlet.MvcResult;

class ProductControllerCreateTest extends ControllerTestSupport {

    private final UUID ownerId = UUID.randomUUID();
    private final UUID nonOwnerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();

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
    }

    @AfterEach
    @Override
    void tearDown() {
        super.tearDown();
        productRepository.deleteAllInBatch();
    }

    @DisplayName("레스토랑의 소유자일경우 관리자 승인 대기, 영업 전, 영업 중, 일시 휴업 상태의 레스토랑에 상품 추가가 가능하다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideOwnerCanAddProductStatuses")
    void shouldAllowOwnerToAddProduct_whenRestaurantStatusIsValid(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        //then
        assertThat(productRepository.count()).isOne();

        String location = mvcResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).isNotNull();

        String productId = location.substring(location.lastIndexOf("/") + 1);
        Optional<Product> after = productRepository.findById(UUID.fromString(productId));
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getQuantity()).isEqualTo(request.getQuantity());
        assertThat(after.get().isAvailable()).isEqualTo(request.getAvailable());
        assertThat(after.get().getPrice().getAmount().compareTo(request.getPrice())).isZero();
    }

    private static Stream<Arguments> provideOwnerCanAddProductStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 폐업, 영업 정지 상태의 레스토랑에는 상품 추가가 불가능하고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideOwnerCannotAddProductStatuses")
    void shouldFailToAddProduct_whenRestaurantStatusIsClosedOrSuspended(String status,
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

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("상품을 관리할 권한이 없습니다."));

        assertThat(productRepository.count()).isZero();
    }

    private static Stream<Arguments> provideOwnerCannotAddProductStatuses() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑의 소유자가 아니라면 상품 생성이 불가능하고 403을 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenUserIsNotOwner() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(nonOwnerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("상품을 생성할 권한이 없습니다."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("레스토랑이 존재하지 않으면 상품 생성이 불가능하고 404를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenRestaurantDoesNotExist() throws Exception {
        //given
        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + UUID.randomUUID() + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("레스토랑이 삭제되었으면 상품 생성이 불가능하고 404를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenRestaurantIsDeleted() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.DELETED)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 정보를 찾을 수 없습니다."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("소유자 정보가 존재하지 않으면 상품 생성이 불가능하고 404를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenRestaurantIsDeleted1() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("레스토랑 오너 정보를 찾을 수 없습니다."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("상품 생성시 상품 이름은 필수이다.")
    @Test
    void shouldFailToCreateProduct_whenNameIsMissing() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 상품 이름을 입력해주세요. (2자 이상 30자 이하)"));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("상품 생성시 상품 이름은 필수이다.")
    @Test
    void shouldFailToCreateProduct_whenPriceIsMissing() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .available(true)
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("price: 상품 가격을 입력해주세요."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("상품 생성시 판매 여부는 필수이다.")
    @Test
    void shouldFailToCreateProduct_whenAvailableIsMissing() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .quantity(100)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("available: 상품 판매 여부를 선택해주세요."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("상품 생성시 재고는 필수이다.")
    @Test
    void shouldFailToCreateProduct_whenQuantityIsMissing() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("quantity: 상품 재고를 입력해주세요."));

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("상품 생성시 재고는 0 이상이어야한다.")
    @Test
    void shouldReturnBadRequest_whenQuantityIsNegative() throws Exception {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        String token = buildToken(ownerId, "access", issuer, Instant.now().plusSeconds(10000));

        CreateProductRequest request = CreateProductRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(-1)
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/restaurants/" + restaurantId + "/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("quantity: 상품 재고는 0 이상이어야 합니다."));

        assertThat(productRepository.count()).isZero();
    }

}
