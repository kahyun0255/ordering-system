package com.orderingsystem.order.application.port.out;

import com.orderingsystem.order.application.dto.request.ValidationCouponApplicationRequest;
import com.orderingsystem.order.application.dto.response.CouponValidateResponse;
import java.util.UUID;

public interface CouponApi {

    CouponValidateResponse validateCoupons(ValidationCouponApplicationRequest request, UUID sagaId);

}
