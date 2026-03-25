package com.orderingsystem.order.application.outbox.coupon.scheduler;

import com.orderingsystem.order.application.outbox.coupon.CouponOutboxHelper;
import com.orderingsystem.outbox.OutboxScheduler;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponOutboxCleanerScheduler implements OutboxScheduler {

    private final CouponOutboxHelper couponOutboxHelper;

    @Value("${outbox.delete-ttl}")
    private Long deleteTtl;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(deleteTtl);
        int deleted = couponOutboxHelper.deleteOlderThan(threshold);

        log.info("{}개의 Order CouponOutbox Message 삭제", deleted);
    }
}
