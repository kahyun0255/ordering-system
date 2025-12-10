package com.orderingsystem.coupon.application;

import com.orderingsystem.coupon.application.dto.response.IssuedCouponResponse;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.coupon.domain.model.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindCouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional
    public List<IssuedCouponResponse> getIssuedCoupons(UUID userId, List<IssuedCouponStatus> couponStatus) {
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findByUserIdAndStatusIn(userId, couponStatus);

        List<UUID> couponIds = issuedCoupons.stream()
                .map(IssuedCoupon::getCouponId)
                .toList();

        List<Coupon> coupons = couponRepository.findAllByCouponIdIn(couponIds);

        Map<UUID, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(Coupon::getCouponId, coupon -> coupon));

        return issuedCoupons.stream()
                .map(issuedCoupon -> {
                    Coupon coupon = couponMap.get(issuedCoupon.getCouponId());

                    return IssuedCouponResponse.builder()
                            .couponId(issuedCoupon.getCouponId())
                            .couponName(coupon.getName())
                            .issuedCouponId(issuedCoupon.getId())
                            .issuedCouponStatus(issuedCoupon.getStatus())
                            .issuedAt(issuedCoupon.getIssuedAt())
                            .usedAt(issuedCoupon.getUsedAt())
                            .expiredAt(issuedCoupon.getExpiredAt())
                            .discountType(coupon.getDiscountType())
                            .amountOff(coupon.getAmountOff())
                            .percentOff(coupon.getPercentOff())
                            .maxDiscountAmount(coupon.getMaxDiscountAmount())
                            .minOrderAmount(coupon.getMinDiscountAmount())
                            .build();
                }).toList();
    }

}
