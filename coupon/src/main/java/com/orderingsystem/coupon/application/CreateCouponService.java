package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.application.dto.request.CreateCouponApplicationRequest;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateCouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public UUID create(CreateCouponApplicationRequest request, UUID userId) {
        log.info("[{}] 유저가 쿠폰 생성 요청. 쿠폰 정보 : [{}]", userId, request.toString());

        Coupon coupon = Coupon.create(request.getDiscountType(), request.getAmountOff(), request.getPercentOff(),
                request.getMaxDiscountAmount(), request.getMinDiscountAmount(), request.getValidFrom(),
                request.getValidUntil(), request.getIssueLimit(), request.getName());

        couponRepository.save(coupon);

        return coupon.getCouponId();
    }

}
