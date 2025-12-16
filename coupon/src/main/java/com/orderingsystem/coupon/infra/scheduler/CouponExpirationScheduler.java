package com.orderingsystem.coupon.infra.scheduler;

import com.orderingsystem.coupon.domain.repository.CouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponExpirationScheduler {

    private final CouponRepository couponRepository;

    @Scheduled(cron = "${scheduler.coupon.expiration}")
    @Transactional
    public void expireCoupons(){
        log.info("쿠폰 만료 정리 배치 처리 시작...");

        int count = couponRepository.bulkExpireCoupons(LocalDateTime.now());

        log.info("총 {}개의 만료된 쿠폰을 EXPIRED 상태로 변경.", count);
    }

}
