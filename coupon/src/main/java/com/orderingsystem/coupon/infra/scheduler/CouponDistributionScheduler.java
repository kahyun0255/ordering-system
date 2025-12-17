package com.orderingsystem.coupon.infra.scheduler;

import com.orderingsystem.coupon.application.IssueCouponService;
import com.orderingsystem.coupon.application.port.out.CouponSchedulerPort;
import com.orderingsystem.coupon.domain.model.CouponStatus;
import com.orderingsystem.coupon.infra.redis.RedisCouponRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponDistributionScheduler implements CouponSchedulerPort {

    private final TaskScheduler taskScheduler;
    private final RedisCouponRepository redisCouponRepository;
    private final IssueCouponService issueCouponService;

    @Override
    public void scheduleCouponStart(UUID couponId, Long issueLimit, LocalDateTime validFrom, LocalDateTime validUntil) {
        LocalDateTime now = LocalDateTime.now();

        if (validFrom.isBefore(now)) {
            registerToRedis(couponId, issueLimit, validUntil);
        } else {
            Instant startTime = validFrom.atZone(ZoneId.systemDefault()).toInstant();

            taskScheduler.schedule(() -> {
                log.info("예약 쿠폰 배포 시작. couponId : [{}]", couponId);
                registerToRedis(couponId, issueLimit, validUntil);
            }, startTime);

            log.info("쿠폰 배포 예약 완료. 실행 시간 : [{}], couponId : [{}]", validFrom, couponId);
        }
    }

    private void registerToRedis(UUID couponId, Long issueLimit, LocalDateTime validUntil) {
        redisCouponRepository.enableCoupon(couponId, issueLimit, validUntil);
        issueCouponService.updateCouponStatus(couponId, CouponStatus.ACTIVE);
    }

}
