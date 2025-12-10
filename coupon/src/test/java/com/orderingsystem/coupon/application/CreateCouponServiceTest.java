package com.orderingsystem.coupon.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

import com.orderingsystem.coupon.application.dto.request.CreateCouponApplicationRequest;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.DiscountType;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CreateCouponService createCouponService;

    @DisplayName("쿠폰 생성 요청이 들어오면 쿠폰을 생성하고 DB에 저장한다.")
    @Test
    void shouldCreateAndPersistCoupon_whenRequestIsValid() {
        //given
        CreateCouponApplicationRequest request = CreateCouponApplicationRequest.builder()
                .discountType(DiscountType.FIXED_AMOUNT)
                .amountOff(BigDecimal.valueOf(2000))
                .minDiscountAmount(BigDecimal.valueOf(10000))
                .validFrom(LocalDateTime.of(2025,12,4,0,0))
                .validUntil(LocalDateTime.of(2025,12,20,0,0))
                .issueLimit(10_000L)
                .build();
        UUID userId = UUID.randomUUID();

        // when
        createCouponService.create(request, userId);

        // then
        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());

        Coupon saved = captor.getValue();
        assertThat(saved.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
        assertThat(saved.getAmountOff()).isEqualByComparingTo("2000");
        assertThat(saved.getPercentOff()).isNull();
        assertThat(saved.getMaxDiscountAmount()).isNull();
        assertThat(saved.getMinDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(saved.getValidFrom()).isEqualTo(LocalDateTime.of(2025,12,4,0,0));
        assertThat(saved.getValidUntil()).isEqualTo(LocalDateTime.of(2025,12,20,0,0));
        assertThat(saved.getIssueLimit()).isEqualTo(10_000L);
    }

}
