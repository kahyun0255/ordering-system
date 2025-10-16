package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.orderingsystem.common.domain.Money;
import com.orderingsystem.restaurant.application.dto.response.ProductResponse;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.ProductRepository;
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

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        restaurantProductPermissionCheckerService = mock(RestaurantProductPermissionCheckerService.class);
        findProductService = new FindProductService(productRepository, restaurantRepository,
                restaurantProductPermissionCheckerService);
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
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("레스토랑 정보를 찾을 수 없습니다.");
    }

}
