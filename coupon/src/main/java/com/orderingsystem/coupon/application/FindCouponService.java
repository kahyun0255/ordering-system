package com.orderingsystem.coupon.application;

import com.orderingsystem.common.exception.AccessDeniedException;
import com.orderingsystem.coupon.application.dto.response.CouponResponse;
import com.orderingsystem.coupon.application.dto.response.IssuedCouponResponse;
import com.orderingsystem.coupon.domain.exception.CouponNotFoundException;
import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.model.IssuedCoupon;
import com.orderingsystem.common.domain.status.IssuedCouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import com.orderingsystem.coupon.domain.repository.IssuedCouponRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindCouponService {

    private final CouponRepository couponRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(UUID userId, List<CouponStatus> couponStatuses) {
        log.info("[{}] 유저가 쿠폰 조회.", userId);

        Set<CouponStatus> searchStatuses = new HashSet<>(couponStatuses);
        if (couponStatuses.contains(CouponStatus.EXPIRED)) {
            searchStatuses.add(CouponStatus.ACTIVE);
        }

        List<Coupon> coupons = couponRepository.findAllByStatusIn(searchStatuses);

        return coupons.stream()
                .map(this::buildCouponResponse)
                .filter(res -> couponStatuses.contains(res.getCouponStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(UUID userId, UUID couponId) {
        Coupon coupon = findCoupon(userId, couponId);
        return buildCouponResponse(coupon);
    }

    @Transactional(readOnly = true)
    public List<IssuedCouponResponse> getIssuedCoupons(UUID userId, List<IssuedCouponStatus> couponStatuses) {
        log.info("[{}] 유저가 발급된 쿠폰 조회.", userId);

        Set<IssuedCouponStatus> searchStatuses = new HashSet<>(couponStatuses);
        if (couponStatuses.contains(IssuedCouponStatus.EXPIRED)) {
            searchStatuses.add(IssuedCouponStatus.ISSUED);
        }

        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findByUserIdAndStatusIn(userId, searchStatuses);

        if (issuedCoupons.isEmpty()) {
            return List.of();
        }

        List<UUID> couponIds = issuedCoupons.stream()
                .map(IssuedCoupon::getCouponId)
                .toList();

        List<Coupon> coupons = couponRepository.findAllByCouponIdIn(couponIds);

        Map<UUID, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(Coupon::getCouponId, coupon -> coupon));

        return issuedCoupons.stream()
                .map(issuedCoupon -> {
                    Coupon coupon = couponMap.get(issuedCoupon.getCouponId());
                    return buildIssueCouponResponse(issuedCoupon, coupon);
                })
                .filter(res -> couponStatuses.contains(res.getIssuedCouponStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public IssuedCouponResponse getIssuedCoupon(UUID userId, Long issuedCouponId) {
        IssuedCoupon issuedCoupon = findIssuedCoupon(userId, issuedCouponId);
        ensureUserOwnsIssuedCoupon(userId, issuedCoupon);

        Coupon coupon = findCoupon(userId, issuedCoupon.getCouponId());

        return buildIssueCouponResponse(issuedCoupon, coupon);
    }

    private IssuedCoupon findIssuedCoupon(UUID userId, Long issuedCouponId) {
        Optional<IssuedCoupon> issuedCoupon = issuedCouponRepository.findById(issuedCouponId);
        if (issuedCoupon.isEmpty()) {
            log.info("[{}] 유저가 발급한 쿠폰 정보가 존재하지 않는 쿠폰 발급 요청. issuedCouponId : [{}]", userId, issuedCouponId);
            throw new CouponNotFoundException("발급한 쿠폰 정보가 존재하지 않습니다.");
        }

        return issuedCoupon.get();
    }

    private void ensureUserOwnsIssuedCoupon(UUID userId, IssuedCoupon issuedCoupon) {
        if (!issuedCoupon.getUserId().equals(userId)) {
            log.info("[{}] 유저가 본인이 발급하지 않은 발급 쿠폰 정보 조회 요청. issuedCouponId : [{}]", userId, issuedCoupon.getId());
            throw new AccessDeniedException("발급한 쿠폰 정보를 조회할 권한이 없습니다.");
        }
    }

    private Coupon findCoupon(UUID userId, UUID couponId) {
        Optional<Coupon> coupon = couponRepository.findById(couponId);
        if (coupon.isEmpty()) {
            log.info("[{}] 유저가 존재하지 않는 쿠폰 [{}] 조회 요청.", userId, couponId);
            throw new CouponNotFoundException("쿠폰이 존재하지 않습니다.");
        }

        return coupon.get();
    }

    private CouponResponse buildCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getCouponId())
                .couponName(coupon.getName())
                .discountType(coupon.getDiscountType())
                .couponStatus(coupon.getDisplayStatus())
                .amountOff(coupon.getAmountOff())
                .percentOff(coupon.getPercentOff())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minDiscountAmount(coupon.getMinDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .validDays(coupon.getValidDays())
                .issueLimit(coupon.getIssueLimit())
                .issuedCount(coupon.getIssuedCount())
                .build();
    }

    private IssuedCouponResponse buildIssueCouponResponse(IssuedCoupon issuedCoupon, Coupon coupon) {
        return IssuedCouponResponse.builder()
                .couponId(issuedCoupon.getCouponId())
                .couponName(coupon.getName())
                .issuedCouponId(issuedCoupon.getId())
                .issuedCouponStatus(issuedCoupon.getDisplayStatus())
                .issuedAt(issuedCoupon.getIssuedAt())
                .usedAt(issuedCoupon.getUsedAt())
                .expiredAt(issuedCoupon.getExpiredAt())
                .discountType(coupon.getDiscountType())
                .amountOff(coupon.getAmountOff())
                .percentOff(coupon.getPercentOff())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinDiscountAmount())
                .build();
    }

}
