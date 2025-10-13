package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.application.dto.request.CreateProductApplicationRequest;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Owner;
import com.orderingsystem.restaurant.domain.model.Product;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantOwnership;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class ProductFacadeCreateTest extends ApplicationTestSupport {

    @Autowired
    private ProductFacade productFacade;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID nonOwnerId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();

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

        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when
        UUID productId = productFacade.create(request, ownerId, restaurantId);

        //then
        Optional<Product> after = productRepository.findById(productId);
        assertThat(after).isPresent();
        assertThat(after.get().getQuantity()).isEqualTo(request.getQuantity());
        assertThat(after.get().getName()).isEqualTo(request.getName());
    }

    private static Stream<Arguments> provideOwnerCanAddProductStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 폐업, 영업 정지 상태의 레스토랑에는 상품 추가가 불가능하고 예외를 반환한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideOwnerCannotAddProductStatuses")
    void shouldFailToAddProduct_whenRestaurantStatusIsClosedOrSuspended(String status,
                                                                        RestaurantStatus restaurantStatus) {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();
        restaurantRepository.save(restaurant);

        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.create(request, ownerId, restaurantId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("상품을 관리할 권한이 없습니다.");

        assertThat(productRepository.count()).isZero();
    }

    private static Stream<Arguments> provideOwnerCannotAddProductStatuses() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑의 소유자가 아니라면 상품 추가가 불가능하고 예외를 반환한다.")
    @Test
    void shouldFailToAddProduct_whenUserIsNotOwner() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();
        restaurantRepository.save(restaurant);

        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.create(request, nonOwnerId, restaurantId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("상품을 생성할 권한이 없습니다.");

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("레스토랑이 삭제되었으면 상품 생성에 실패하고, 예외를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenRestaurantIsDeleted() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.DELETED)
                .build();
        restaurantRepository.save(restaurant);

        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.create(request, ownerId, restaurantId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("레스토랑이 존재하지 않으면 상품 생성에 실패하고, 예외를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenRestaurantDoesNotExist() {
        //given
        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.create(request, ownerId, UUID.randomUUID()))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");

        assertThat(productRepository.count()).isZero();
    }

    @DisplayName("소유자가 존재하지 않으면 상품 생성에 실패하고, 예외를 반환한다.")
    @Test
    void shouldFailToCreateProduct_whenOwnerIsNotFound() {
        //given
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.DELETED)
                .build();
        restaurantRepository.save(restaurant);

        CreateProductApplicationRequest request = CreateProductApplicationRequest.builder()
                .name("상품 이름")
                .price(BigDecimal.valueOf(1000.00))
                .available(true)
                .quantity(100)
                .build();

        //when, then
        assertThatThrownBy(() -> productFacade.create(request, UUID.randomUUID(), restaurantId))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 오너 정보를 찾을 수 없습니다.");

        assertThat(productRepository.count()).isZero();
    }

}
