package com.orderingsystem.coupon.presentation.request;

import com.orderingsystem.coupon.application.dto.request.CreateCouponApplicationRequest;
import com.orderingsystem.coupon.domain.model.DiscountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateCouponRequest {

    @NotNull(message = "쿠폰 타입은 필수입니다.")
    private DiscountType discountType;

    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    private String name;

    @PositiveOrZero(message = "할인 금액은 0 이상이어야 합니다.")
    private BigDecimal amountOff;

    @Min(value = 1, message = "할인율은 1 이상이어야 합니다.")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
    private Long percentOff;

    @PositiveOrZero(message = "최대 할인 금액은 0 이상이어야 합니다.")
    private BigDecimal maxDiscountAmount;

    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    private BigDecimal minDiscountAmount;

    @NotNull(message = "쿠폰 발행 시작 시간은 필수입니다.")
    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @PositiveOrZero(message = "발행 개수는 0 이상이어야 합니다.")
    private Long issueLimit;

    @AssertTrue(message = "쿠폰 타입이 FIXED_AMOUNT면 amountOff가 필수이며 percentOff, maxDiscountAmount는 비워야 합니다.")
    private boolean isAmountRuleOk() {
        if (discountType == null) return true;
        if (discountType == DiscountType.FIXED_AMOUNT) {
            return amountOff != null && percentOff == null && maxDiscountAmount == null;
        }
        return true;
    }

    @AssertTrue(message = "타입이 PERCENTAGE면 percentOff가 필수이며 amountOff는 비워야 합니다.")
    private boolean isPercentRuleOk() {
        if (discountType == null) return true;
        if (discountType == DiscountType.PERCENTAGE) {
            return percentOff != null && amountOff == null;
        }
        return true;
    }

    @AssertTrue(message = "유효기간 종료는 시작 이후여야 합니다.")
    private boolean isPeriodValid() {
        return validFrom == null || validUntil == null || validUntil.isAfter(validFrom);
    }

    public CreateCouponApplicationRequest toApplicationRequest(){
        return CreateCouponApplicationRequest.builder()
                .discountType(this.discountType)
                .amountOff(this.amountOff)
                .percentOff(this.percentOff)
                .maxDiscountAmount(this.maxDiscountAmount)
                .minDiscountAmount(this.minDiscountAmount)
                .validFrom(this.validFrom)
                .validUntil(this.validUntil)
                .issueLimit(this.issueLimit)
                .name(this.name)
                .build();
    }

}
