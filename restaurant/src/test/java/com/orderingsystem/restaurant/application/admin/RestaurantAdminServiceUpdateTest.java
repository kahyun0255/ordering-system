package com.orderingsystem.restaurant.application.admin;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.orderingsystem.restaurant.application.dto.request.UpdateRestaurantApplicationRequest;
import com.orderingsystem.restaurant.domain.exception.RestaurantNotFoundException;
import com.orderingsystem.restaurant.domain.model.Restaurant;
import com.orderingsystem.restaurant.domain.model.RestaurantStatus;
import com.orderingsystem.restaurant.domain.repository.RestaurantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantAdminServiceUpdateTest {

    @InjectMocks
    private RestaurantAdminService restaurantAdminService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @DisplayName("레스토랑 정보 변경에 성공한다.")
    @Test
    void shouldSucceedUpdatingRestaurant_whenAdminRequestsUpdating() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = mock(Restaurant.class);

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurant.getName()).willReturn("레스토랑 이름");
        given(restaurant.getStatus()).willReturn(RestaurantStatus.ACTIVE);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경할 레스토랑 이름")
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when
        restaurantAdminService.updateRestaurant(userId, restaurantId, request);

        //then
        verify(restaurant, times(1)).updateName(request.getName());
        verify(restaurant, times(1)).updateStatusByAdmin(request.getStatus());
    }

    @DisplayName("기존 이름, 상태와 같은 정보로 변경을 요청하면 변경을 진행하지 않는다.")
    @Test
    void shouldNotProceedUpdate_whenRequestingWithSameNameAndStatus() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String name = "레스토랑 이름";
        RestaurantStatus status = RestaurantStatus.ACTIVE;

        Restaurant restaurant = mock(Restaurant.class);

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurant.getName()).willReturn(name);
        given(restaurant.getStatus()).willReturn(status);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name(name)
                .status(status)
                .build();

        //when
        restaurantAdminService.updateRestaurant(userId, restaurantId, request);

        //then
        verify(restaurant, never()).updateName(request.getName());
        verify(restaurant, never()).updateStatusByAdmin(request.getStatus());
    }

    @DisplayName("레스토랑 이름만 변경할 수 있다.")
    @Test
    void shouldSucceedUpdatingOnlyRestaurantName_whenRequestingUpdate() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = mock(Restaurant.class);

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurant.getName()).willReturn("레스토랑 이름");
        given(restaurant.getStatus()).willReturn(RestaurantStatus.ACTIVE);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경할 레스토랑 이름")
                .build();

        //when
        restaurantAdminService.updateRestaurant(userId, restaurantId, request);

        //then
        verify(restaurant, times(1)).updateName(request.getName());
        verify(restaurant, never()).updateStatusByAdmin(request.getStatus());
    }

    @DisplayName("레스토랑 상태만 변경할 수 있다.")
    @Test
    void shouldSucceedUpdatingOnlyRestaurantStatus_whenRequestingUpdate() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = mock(Restaurant.class);

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(restaurant.getName()).willReturn("레스토랑 이름");
        given(restaurant.getStatus()).willReturn(RestaurantStatus.ACTIVE);

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .status(RestaurantStatus.SUSPENDED)
                .build();

        //when
        restaurantAdminService.updateRestaurant(userId, restaurantId, request);

        //then
        verify(restaurant, never()).updateName(request.getName());
        verify(restaurant, times(1)).updateStatusByAdmin(request.getStatus());
    }

    @DisplayName("레스토랑 이름이 공백일 경우, 예외가 발생한다.")
    @Test
    void shouldFailToUpdateRestaurantName_whenNameIsBlank() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("이름")
                .status(RestaurantStatus.ACTIVE)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("    ")
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantAdminService.updateRestaurant(userId, restaurantId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레스토랑 이름은 비어있을 수 없습니다.");
    }

    @DisplayName("레스토랑이 존재하지 않을경우, 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRestaurantDoesNotExist() {
        //given
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .name("이름")
                .status(RestaurantStatus.ACTIVE)
                .build();

        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        UpdateRestaurantApplicationRequest request = UpdateRestaurantApplicationRequest.builder()
                .name("변경")
                .build();

        //when, then
        assertThatThrownBy(() -> restaurantAdminService.updateRestaurant(userId, restaurantId, request))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessage("레스토랑 정보를 찾을 수 없습니다.");
    }

}
