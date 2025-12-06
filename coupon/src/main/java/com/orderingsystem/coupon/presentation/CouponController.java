package com.orderingsystem.coupon.presentation;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.coupon.application.CouponFacade;
import com.orderingsystem.coupon.presentation.request.CreateCouponRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CommonJwtUtil commonJwtUtil;
    private final CouponFacade couponFacade;

    @PostMapping
    public ResponseEntity<Void> createCoupon(@RequestHeader("Authorization") String authorizationHeader,
                                             @RequestBody @Valid CreateCouponRequest createCouponRequest) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        UserType userType = commonJwtUtil.getUserRoleFromToken(authorizationHeader);
        UUID couponId = couponFacade.createCoupon(createCouponRequest.toApplicationRequest(), userType, userId);
        return ResponseEntity.created(URI.create("/api/coupons/"+couponId)).build();
    }

}
