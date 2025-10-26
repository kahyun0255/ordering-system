package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.exception.ProductNotFoundException;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantProduct;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantProductRepository;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import com.orderingsystem.restaurant.domain.service.RestaurantProductPermissionCheckerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class FindProductServiceTest {

    private ProductRepository productRepository;
    private RestaurantRepository restaurantRepository;
    private RestaurantProductPermissionCheckerService restaurantProductPermissionCheckerService;
    private FindProductService findProductService;
    private RestaurantProductRepository restaurantProductRepository;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        restaurantProductPermissionCheckerService = mock(RestaurantProductPermissionCheckerService.class);
        restaurantProductRepository = mock(RestaurantProductRepository.class);
        findProductService = new FindProductService(productRepository, restaurantRepository,
                restaurantProductPermissionCheckerService, restaurantProductRepository);
    }

    @DisplayName("정상적인 레스토랑 ID로 상품 목록 조회시 ProductResponse Page를 반환한다.")
    @Test
    void shouldReturnProductResponses_whenRestaurantIsValid() {
        //given
        UUID restaurantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build();

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("상품")
                .price(new Money(BigDecimal.valueOf(18000)))
                .available(true)
                .quantity(10)
                .build();

        Page<Product> mockPage = new PageImpl<>(List.of(product));

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);
        given(productRepository.findByRestaurantId(restaurantId, pageable)).willReturn(mockPage);

        //when
        Page<ProductResponse> result = findProductService.findAll(restaurantId, pageable);

        //then
        assertThat(result).hasSize(1);
        ProductResponse response = result.getContent().get(0);
        assertThat(response.getName()).isEqualTo("상품");
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(18000));
    }

    @Test
    @DisplayName("레스토랑이 존재하지 않으면 예외가 발생한다.")
    void shouldThrowException_whenRestaurantDoesNotExist() {
        //given
        UUID restaurantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> findProductService.findAll(restaurantId, pageable))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("레스토랑이 삭제되었으면 예외가 발생한다.")
    void shouldThrowException_whenRestaurantIsDeleted() {
        //given
        UUID restaurantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.DELETED)
                .name("삭제된 레스토랑")
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> findProductService.findAll(restaurantId, pageable))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("레스토랑이 존재하지만 관리 불가능한 상태면 예외가 발생한다.")
    void shouldThrowException_whenRestaurantIsNotManageable() {
        //given
        UUID restaurantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.SUSPENDED)
                .name("정지된 레스토랑")
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> findProductService.findAll(restaurantId, pageable))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("물품을 조회할 권한이 없습니다.");
    }

    @DisplayName("상품 관리가 가능한 상태의 레스토랑에서 판매중인 상품이라면 상품 ID로 상품 조회시 상품 정보를 반환한다.")
    @Test
    void shouldReturnProduct_whenRestaurantIsActiveAndProductIsAvailable() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build();

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("상품")
                .price(new Money(BigDecimal.valueOf(18000)))
                .available(true)
                .quantity(10)
                .build();

        RestaurantProduct restaurantProduct = RestaurantProduct.builder()
                .id(100L)
                .productId(product.getProductId())
                .restaurantId(restaurantId)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);
        given(productRepository.findById(product.getProductId())).willReturn(Optional.of(product));
        given(restaurantProductRepository.findByRestaurantIdAndProductId(restaurantId, product.getProductId()))
                .willReturn(Optional.of(restaurantProduct));

        //when
        ProductResponse response = findProductService.findOne(restaurantId, product.getProductId());

        //then
        assertThat(response.getName()).isEqualTo("상품");
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(18000));
        assertThat(response.getQuantity()).isEqualTo(10);
    }

    @DisplayName("상품 관리가 불가능한 상태의 레스토랑에서 판매하는 상품은 상품ID로 조회가 불가능하고, 예외가 발생한다.")
    @Test
    void shouldDenyProductAccess_whenRestaurantStatusBlocksExposure() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.SUSPENDED)
                .name("레스토랑")
                .build();

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("상품")
                .price(new Money(BigDecimal.valueOf(18000)))
                .available(true)
                .quantity(10)
                .build();

        RestaurantProduct restaurantProduct = RestaurantProduct.builder()
                .id(100L)
                .productId(product.getProductId())
                .restaurantId(restaurantId)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(false);
        given(productRepository.findById(product.getProductId())).willReturn(Optional.of(product));
        given(restaurantProductRepository.findByRestaurantIdAndProductId(restaurantId, product.getProductId()))
                .willReturn(Optional.of(restaurantProduct));

        //when, then
        assertThatThrownBy(()->findProductService.findOne(restaurantId, product.getProductId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("물품을 조회할 권한이 없습니다.");
    }

    @Test
    @DisplayName("상품 조회시 레스토랑이 존재하지 않으면 예외가 발생한다.")
    void shouldThrowException_whenRestaurantDoesNotExist2() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(() -> findProductService.findOne(restaurantId, productId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑 정보를 찾을 수 없습니다.");
    }

    @DisplayName("레스토랑이 삭제되었으면 예외가 발생한다.")
    @Test
    void shouldThrowRestaurantNotFoundException_whenRestaurantIsDeleted() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.DELETED)
                .name("레스토랑")
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);

        //when, then
        assertThatThrownBy(() -> findProductService.findOne(restaurantId, productId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑 정보를 찾을 수 없습니다.");
    }

    @DisplayName("상품이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductDoesNotExist() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);
        given(productRepository.findById(any())).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(()->findProductService.findOne(restaurantId, UUID.randomUUID()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("상품 정보를 찾을 수 없습니다");
    }

    @DisplayName("판매중이지 않은 상태의 상품을 조회하려하면 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenProductIsNotAvailable() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build();

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("상품")
                .price(new Money(BigDecimal.valueOf(18000)))
                .available(false)
                .quantity(10)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);
        given(productRepository.findById(product.getProductId())).willReturn(Optional.of(product));

        //when, then
        assertThatThrownBy(()->findProductService.findOne(restaurantId, product.getProductId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("현재 판매하고 있지 않은 상품입니다.");
    }

    @DisplayName("레스토랑이 판매하고있지 않은 상품은 상품ID로 조회가 불가능하고, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenProductNotMappedToRestaurant() throws Exception {
        //given
        UUID restaurantId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .status(RestaurantStatus.ACTIVE)
                .name("레스토랑")
                .build();

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("상품")
                .price(new Money(BigDecimal.valueOf(18000)))
                .available(true)
                .quantity(10)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurantProductPermissionCheckerService.canManageProduct(restaurant)).willReturn(true);
        given(productRepository.findById(product.getProductId())).willReturn(Optional.of(product));
        given(restaurantProductRepository.findByRestaurantIdAndProductId(restaurantId, product.getProductId()))
                .willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(()->findProductService.findOne(restaurantId, product.getProductId()))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("해당 레스토랑이 판매하고 있지 않은 상품입니다.");
    }

}
