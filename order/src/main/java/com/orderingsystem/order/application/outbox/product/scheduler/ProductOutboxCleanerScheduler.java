package com.orderingsystem.order.application.outbox.product.scheduler;

import com.orderingsystem.order.application.outbox.product.ProductOutboxHelper;
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
public class ProductOutboxCleanerScheduler implements OutboxScheduler {

    private final ProductOutboxHelper productOutboxHelper;

    @Value("${outbox.delete-ttl}")
    private Long deleteTtl;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(deleteTtl);
        int deleted = productOutboxHelper.deleteOlderThan(threshold);

        log.info("{}개의 Order PaymentOutbox Message 삭제", deleted);
    }
}
