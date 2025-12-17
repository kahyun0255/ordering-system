package com.orderingsystem.coupon.infra.scheduler;

import com.orderingsystem.coupon.domain.model.Coupon;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStartRunner implements ApplicationRunner {

    private final CouponRepository couponRepository;
    private final CouponDistributionScheduler couponDistributionScheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("서버 재시작. 예정된 쿠폰 스케줄 복구 시작.");

        LocalDateTime now = LocalDateTime.now();

        List<Coupon> futureCoupons = couponRepository.findAllByStatusAndValidUntilAfter(CouponStatus.SCHEDULED, now);

        for (Coupon coupon : futureCoupons) {
            couponDistributionScheduler.scheduleCouponStart(coupon.getCouponId(), coupon.getIssueLimit(),
                    coupon.getValidFrom(), coupon.getValidUntil());
        }
    }

}
