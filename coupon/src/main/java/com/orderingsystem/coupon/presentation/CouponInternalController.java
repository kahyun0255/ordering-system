package com.orderingsystem.coupon.presentation;

import com.orderingsystem.coupon.application.CouponManagementService;
import com.orderingsystem.coupon.application.dto.response.CouponValidateResponse;
import com.orderingsystem.coupon.presentation.request.ValidationCouponRequest;
import javax.print.DocFlavor.READER;
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

    private final CouponManagementService couponManagementService;

    @PostMapping("/validation")
    private ResponseEntity<CouponValidateResponse> validateCoupons(@RequestBody ValidationCouponRequest request){
        return ResponseEntity.ok(couponManagementService.aa(request));
    }

}
