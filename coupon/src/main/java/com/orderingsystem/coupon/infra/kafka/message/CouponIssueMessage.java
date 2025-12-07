package com.orderingsystem.coupon.infra.kafka.message;

import com.orderingsystem.coupon.application.dto.request.CouponIssueApplicationRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage {

    private UUID couponId;
    private UUID userId;
    private LocalDateTime issuedAt;

    public CouponIssueApplicationRequest toCouponIssueApplicationRequest(){
        return CouponIssueApplicationRequest.builder()
                .couponId(this.couponId)
                .userId(this.userId)
                .issuedAt(this.issuedAt)
                .build();
    }

}
