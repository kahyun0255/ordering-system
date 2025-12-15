package com.orderingsystem.coupon.presentation;

import com.orderingsystem.common.domain.status.UserType;
import com.orderingsystem.common.util.CommonJwtUtil;
import com.orderingsystem.coupon.application.CouponFacade;
import com.orderingsystem.coupon.application.FindCouponService;
import com.orderingsystem.coupon.application.IssueCouponService;
import com.orderingsystem.coupon.application.dto.response.CouponResponse;
import com.orderingsystem.coupon.application.dto.response.IssuedCouponResponse;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import com.orderingsystem.coupon.presentation.request.CreateCouponRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponController {

    private final CommonJwtUtil commonJwtUtil;
    private final CouponFacade couponFacade;
    private final IssueCouponService issueCouponService;
    private final FindCouponService findCouponService;

    @PostMapping
    public ResponseEntity<Void> createCoupon(@RequestHeader("Authorization") String authorizationHeader,
                                             @RequestBody @Valid CreateCouponRequest createCouponRequest) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        UserType userType = commonJwtUtil.getUserRoleFromToken(authorizationHeader);
        UUID couponId = couponFacade.createCoupon(createCouponRequest.toApplicationRequest(), userType, userId);
        return ResponseEntity.created(URI.create("/api/coupons/" + couponId)).build();
    }

    @PostMapping("/{couponId}/claims")
    public ResponseEntity<Void> issueCoupon(@RequestHeader("Authorization") String authorizationHeader,
                                            @PathVariable UUID couponId) {
        LocalDateTime issuanceRequestedAt = LocalDateTime.now();
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        issueCouponService.requestCouponIssuance(couponId, userId, issuanceRequestedAt);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/issued")
    public ResponseEntity<List<IssuedCouponResponse>> getIssuedCoupons(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "status", defaultValue = "ISSUED") List<IssuedCouponStatus> couponStatuses) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(findCouponService.getIssuedCoupons(userId, couponStatuses));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getCoupons(@RequestHeader("Authorization") String authorizationHeader,
                                                           @RequestParam(value = "status", defaultValue = "ACTIVE") List<CouponStatus> couponStatuses) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(findCouponService.getCoupons(userId, couponStatuses));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(@RequestHeader("Authorization") String authorizationHeader,
                                            @PathVariable UUID couponId) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(findCouponService.getCoupon(userId, couponId));
    }

}
