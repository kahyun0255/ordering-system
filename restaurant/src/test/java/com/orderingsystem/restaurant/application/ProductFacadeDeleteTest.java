package com.orderingsystem.restaurant.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductFacadeDeleteTest {

    @Mock
    private DeleteProductService deleteProductService;

    @Mock
    private RestaurantAccessValidatorService restaurantAccessValidatorService;

    @InjectMocks
    private ProductFacade productFacade;

    @Mock
    private StockCachePort stockCachePort;

    @DisplayName("레스토랑의 소유자이고, 해당 레스토랑이 '관리자 승인 대기', '영업 전', '영업 중', '일시 휴업' 상태라면 상품을 삭제할 수 있다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("provideDeletableRestaurantStatuses")
    void shouldDeleteProduct_whenOwnerAndRestaurantStatusAllows(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId =UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();

        given(restaurantAccessValidatorService.findRestaurant(restaurantId)).willReturn(restaurant);
        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);

        //when
        productFacade.delete(restaurantId, productId, ownerId);

        //then
        verify(deleteProductService).deleteProduct(restaurantId, productId, ownerId);
    }

    private static Stream<Arguments> provideDeletableRestaurantStatuses() {
        return Stream.of(
                Arguments.of("관리자 승인 대기", RestaurantStatus.PENDING_APPROVAL),
                Arguments.of("영업 전", RestaurantStatus.PRE_OPEN),
                Arguments.of("영업 중", RestaurantStatus.ACTIVE),
                Arguments.of("일시 휴업", RestaurantStatus.TEMP_CLOSED)
        );
    }

    @DisplayName("레스토랑의 소유자라도 해당 레스토랑이 '폐업', '영업 정지' 상태라면 상품을 삭제할 수 없고 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 레스토랑 상태 : {0}")
    @MethodSource("restrictedRestaurantStatusesForProductDeletion")
    void shouldDenyProductDeletion_whenRestaurantStatusIsRestricted(String status, RestaurantStatus restaurantStatus)
            throws Exception {
        //given
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId =UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(restaurantStatus)
                .build();

        given(restaurantAccessValidatorService.findRestaurant(restaurantId)).willReturn(restaurant);
        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(true);

        //when, then
        assertThatThrownBy(()-> productFacade.delete(restaurantId, productId, ownerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("현재 상태의 레스토랑에서는 상품을 관리할 수 없습니다.");
    }

    private static Stream<Arguments> restrictedRestaurantStatusesForProductDeletion() {
        return Stream.of(
                Arguments.of("폐업", RestaurantStatus.PERM_CLOSED),
                Arguments.of("영업 정지", RestaurantStatus.SUSPENDED)
        );
    }

    @DisplayName("레스토랑 소유자가 아니면 상품 삭제시 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIsNotOwner() {
        //given
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId =UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("레스토랑")
                .status(RestaurantStatus.ACTIVE)
                .build();

        given(restaurantAccessValidatorService.findRestaurant(restaurantId)).willReturn(restaurant);
        given(restaurantAccessValidatorService.isRestaurantOwnership(ownerId, restaurantId)).willReturn(false);

        //when, then
        assertThatThrownBy(()-> productFacade.delete(restaurantId, productId, ownerId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("상품을 관리할 권한이 없습니다.");
    }

}
