package com.orderingsystem.coupon.presentation;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.coupon.application.CouponFacade;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponManagementController {

    private final CommonJwtUtil commonJwtUtil;
    private final CouponFacade couponFacade;

    @PostMapping("/{couponId}/pause")
    public ResponseEntity<Void> pauseCoupon(@RequestHeader("Authorization") String authorizationHeader,
                                            @PathVariable UUID couponId) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        UserType userType = commonJwtUtil.getUserRoleFromToken(authorizationHeader);
        couponFacade.pauseCoupon(couponId, userId, userType);
        return ResponseEntity.ok().build();
    }

}
