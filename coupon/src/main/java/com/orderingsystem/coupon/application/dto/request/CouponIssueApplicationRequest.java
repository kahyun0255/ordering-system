package com.orderingsystem.coupon.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponIssueApplicationRequest {

    private UUID couponId;
    private UUID userId;
    private LocalDateTime issuedAt;

}
