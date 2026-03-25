package com.orderingsystem.order.application.port.out;

import com.orderingsystem.order.application.dto.request.ValidationCouponApplicationRequest;
import com.orderingsystem.order.application.dto.response.CouponValidationResponse;
import java.util.UUID;

public interface CouponApi {

    CouponValidationResponse validateCoupons(ValidationCouponApplicationRequest request, UUID sagaId);

}
