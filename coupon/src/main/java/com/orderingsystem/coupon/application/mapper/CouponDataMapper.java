package com.orderingsystem.coupon.application.mapper;

import com.orderingsystem.coupon.application.dto.request.CouponRequest;
import com.orderingsystem.coupon.application.outbox.order.model.CouponOrderEventPayload;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CouponDataMapper {

    public CouponOrderEventPayload redeemCouponToCouponOrderEventPayload(CouponRequest request,
                                                                         int updatedCount,
                                                                         List<String> failureMessage) {
        return CouponOrderEventPayload.builder()
                .orderId(request.getOrderId().toString())
                .customerId(request.getUserId().toString())
                .sagaId(request.getSagaId().toString())
                .issuedCouponId(request.getIssuedCouponIds().stream().map(Object::toString).toList())
                .createdAt(ZonedDateTime.now())
                .failureMessage(failureMessage)
                .updatedCount(updatedCount)
                .build();
    }

}
