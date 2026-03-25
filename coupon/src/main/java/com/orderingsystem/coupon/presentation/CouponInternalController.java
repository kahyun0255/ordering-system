package com.orderingsystem.coupon.presentation;

import com.orderingsystem.coupon.application.CouponValidationService;
import com.orderingsystem.coupon.application.dto.response.CouponValidationResponse;
import com.orderingsystem.coupon.presentation.request.CouponValidationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/coupons")
public class CouponInternalController {

    private final CouponValidationService couponValidationService;

    @PostMapping("/validation")
    private ResponseEntity<CouponValidationResponse> validateCoupons(@RequestBody CouponValidationRequest request){
        return ResponseEntity.ok(couponValidationService.validateAndCalculate(request));
    }

}
