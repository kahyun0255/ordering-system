package com.orderingsystem.coupon.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.coupon.application.dto.request.CreateCouponApplicationRequest;
import com.orderingsystem.coupon.application.port.out.CouponCachePort;
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
class CouponFacadeTest {

    @Mock
    private CreateCouponService createCouponService;

    @Mock
    private CouponManagementService couponManagementService;

    @Mock
    private CouponCachePort couponCachePort;

    @InjectMocks
    private CouponFacade couponFacade;

    @DisplayName("관리자가 쿠폰 생성 요청시 쿠폰 생성을 시도한다.")
    @Test
    void shouldAttemptToCreateCoupon_whenUserIsAdmin() {
        //given
        CreateCouponApplicationRequest request = mock(CreateCouponApplicationRequest.class);
        UserType userType = UserType.ADMIN;
        UUID userId = UUID.randomUUID();

        //when
        couponFacade.createCoupon(request, userType, userId);

        //then
        verify(createCouponService).create(request, userId);
    }

    @DisplayName("관리자가 아닌 유저가 쿠폰 생성시 쿠폰 생성이 불가능하고, 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldThrowException_whenNonAdminTriesToCreateCoupon(String type, UserType userType) {
        //given
        CreateCouponApplicationRequest request = mock(CreateCouponApplicationRequest.class);
        UUID userId = UUID.randomUUID();

        //when, then
        assertThatThrownBy(()->couponFacade.createCoupon(request, userType, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("쿠폰 생성이 불가능합니다.");
    }

    @DisplayName("관리자가 쿠폰을 정지하려하면 쿠폰 정지를 시도한다.")
    @Test
    void shouldAttemptToDeactivateCoupon_whenUserHasAdminRole() {
        //given
        UUID couponId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        //when
        couponFacade.pauseCoupon(couponId, userId, UserType.ADMIN);

        //then
        verify(couponManagementService).pause(couponId, userId);
    }

    @DisplayName("관리자가 아닌 유저가 쿠폰을 정지하려 하면 예외가 발생한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideNonAdminRoles")
    void shouldThrowException_whenNonAdminTriesToDeactivateCoupon(String type, UserType userType) {
        //when, then
        assertThatThrownBy(()->couponFacade.pauseCoupon(UUID.randomUUID(), UUID.randomUUID(), userType))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("쿠폰 정지가 불가능합니다.");

    }

    private static Stream<Arguments> provideNonAdminRoles() {
        return Stream.of(
                Arguments.of("레스토랑 소유자", UserType.RESTAURANT_OWNER),
                Arguments.of("구매자", UserType.CUSTOMER)
        );
    }

}
