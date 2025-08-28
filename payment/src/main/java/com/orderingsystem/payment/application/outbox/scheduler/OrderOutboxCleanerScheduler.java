package com.orderingsystem.payment.application.outbox.scheduler;

import com.orderingsystem.outbox.OutboxScheduler;
import com.orderingsystem.payment.application.outbox.OrderOutboxHelper;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxCleanerScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;

    @Value("${outbox.delete-ttl}")
    private Long deleteTtl;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(deleteTtl);
        int deleted = orderOutboxHelper.deleteOlderThan(threshold);

        log.info("{}개의 Payment OrderOutbox Message 삭제", deleted);
    }
}
